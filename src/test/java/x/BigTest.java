package x;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

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
                    return () -> writerThread(campaignIdStart, campaignIdEnd, usersPerCampaign);
                });

        Stream<Runnable> readerThreadRunnables = IntStream
                .range(0, 5)
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

    private void writerThread(long campaignIdStart, long campaignIdEnd, int usersPerCampaign) {
        for (long i = campaignIdStart; i < campaignIdEnd; i++) {
            writeCampaign(i, usersPerCampaign);
        }
    }

    private void writeCampaign(long campaignId, int usersPerCampaign) {

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
