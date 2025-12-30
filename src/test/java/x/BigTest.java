package x;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// todo Может быть нужно добавить больше потоков в контейнер с PG и настроить больше воркеров в самом PG.
@SpringBootTest
public class BigTest {

    @Autowired
    private MessageJdbcRepository messageJdbcRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void test() {
        Long maxCampaignId = getMaxCampaignId();
        if (maxCampaignId == null) {
            maxCampaignId = -1L;
        }

        long firstCampaignId = maxCampaignId + 1;
        int campaignsNumber = 10;
        int usersPerCampaign = 100_000;
        int maxCampaignsPerThread = 2;
        int threadsNumber = (int) Math.ceil(1. * campaignsNumber / maxCampaignsPerThread);

        // Генерируем базу для новых рассылок
        jdbcTemplate.update(
                """
                call gen_campaign_users(
                    p_start_campaign_id => ?,
                    p_campaigns_number => ?,
                    p_users_per_campaign => ?
                )
                """,
                firstCampaignId,
                campaignsNumber,
                usersPerCampaign
        );

        Stream<Runnable> writerThreadRunnables = IntStream
                .range(0, threadsNumber)
                .mapToObj(threadIndex -> {
                    int campaignIndexStart = threadIndex * maxCampaignsPerThread;
                    int campaignIndexEnd = Math.min(campaignsNumber, campaignIndexStart + maxCampaignsPerThread);
                    long campaignIdStart = firstCampaignId + campaignIndexStart;
                    long campaignIdEnd = firstCampaignId + campaignIndexEnd;
                    return () -> writerThread(campaignIdStart, campaignIdEnd);
                });

        Stream<Runnable> readerThreadRunnables = IntStream
                .range(0, 0)
                .mapToObj(it -> this::readerThread);

        runThreads(Stream.concat(
                writerThreadRunnables,
                readerThreadRunnables
        ));
    }

    private void readerThread() {
        while (true) {
            messageJdbcRepository.getUserMessages(55L);
            if (Thread.interrupted()) return;
        }
    }

    private void writerThread(long campaignIdStart, long campaignIdEnd) {
        for (long i = campaignIdStart; i < campaignIdEnd; i++) {
            writeCampaign(i);
            if (Thread.interrupted()) return;
        }
    }

    private void writeCampaign(long campaignId) {
        List<Message> messages = jdbcTemplate.query(
                """
                select
                    user_id
                from
                    campaign_users
                where
                    campaign_id = ?
                """,
                (rs, n) -> {

                    Message message = new Message();
                    message.id_uuid = UUID.randomUUID();
                    message.campaign_id = campaignId;
                    message.user_id = rs.getLong(1);
                    message.topic = "Кампания " + campaignId;
                    message.created = Instant.now();
                    message.sent = null;
                    message.deleted = null;

                    message.text =
                            Stream
                                    .generate(() ->
                                            "текст кампании " + campaignId
                                    )
                                    .limit(20)
                                    .collect(Collectors.joining(", "));

                    return message;
                },
                campaignId
        );

        messageJdbcRepository.batchInsert(messages, 100);
    }

    private void runThreads(Stream<Runnable> runnables) {
        runnables
                .map(Thread::new)
                .peek(Thread::start)
                .toList()
                .forEach(it -> {
                    try {
                        it.join();
                    } catch (InterruptedException ignored) {
                    }
                })
        ;
    }

    private @Nullable Long getMaxCampaignId() {
        return jdbcTemplate.queryForObject("select max(campaign_id) from campaign_users", Long.class);
    }
}
