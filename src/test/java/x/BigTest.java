package x;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.transaction.support.TransactionTemplate;
import x.reader.ListByUserMessagesTimerWrapper;
import x.reader.MessageReader;
import x.reader.MessageReaderSelector;
import x.reader.MessageReaderType;
import x.writer.BatchInsertTimerWrapper;
import x.writer.MessageWriter;
import x.writer.MessageWriterSelector;
import x.writer.MessageWriterType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMetrics
public class BigTest {

    public static final Logger logger = LoggerFactory.getLogger(BigTest.class);

    private static final int MAX_BATCH_SIZE = 1024;

    private static final int WRITER_THREADS_NUMBER = 1;
    private static final int CAMPAIGNS_PER_THREAD = 1;
    private static final int CAMPAIGNS_NUMBER = WRITER_THREADS_NUMBER * CAMPAIGNS_PER_THREAD;
    private static final int USERS_PER_CAMPAIGN = 100_000;
    private static final int USERS_NUMBER = 1_000_000;
    private static final int MESSAGE_TABLE_SIZE = 70_000_000;

    private static final int READER_THREADS_NUMBER = 5;

    //private static final MessageWriterType MESSAGE_WRITER_TYPE = MessageWriterType.JDBC_UNNEST;
    private static final MessageWriterType MESSAGE_WRITER_TYPE = MessageWriterType.JDBC_REWRITE_BATCHED_INSERTS;

    private static final MessageReaderType MESSAGE_READER_TYPE = MessageReaderType.SPRING_DATA_JPA;

    @Autowired
    private MessageWriterSelector messageWriterSelector;

    @Autowired
    private MessageReaderSelector messageReaderSelector;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private BatchInsertTimerWrapper batchInsertTimerWrapper;

    @Autowired
    private ListByUserMessagesTimerWrapper listByUserMessagesTimerWrapper;

    private List<Tag> constantTags;

    private MessageWriter messageWriter;

    private MessageReader messageReader;

    private void execute(StatementCallback<?> action) throws DataAccessException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                action.doInStatement(statement);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @org.junit.jupiter.api.Tag("manual")
    public void test() {

        messageWriter = messageWriterSelector.get(MESSAGE_WRITER_TYPE);
        messageReader = messageReaderSelector.get(MESSAGE_READER_TYPE);

        logger.info("rollback_messages...");
        transactionTemplate.execute((status) -> {
            jdbcTemplate.update(
                    """
                    call rollback_messages(
                        p_campaigns_number => 700,
                        p_users_per_campaign => 100000
                    )
                    """
            );
            return null;
        });

        logger.info("vacuum (analyze) message");
        execute(st -> {
            st.execute("vacuum (analyze) message");
            return null;
        });

        logger.info("vacuum (analyze) campaign_users");
        execute(st -> {
            st.execute("vacuum (analyze) campaign_users");
            return null;
        });

        Long maxCampaignId = getMaxCampaignId();
        if (maxCampaignId == null) {
            maxCampaignId = 0L;
        }

        long firstCampaignId = maxCampaignId + 1;

        logger.info("firstCampaignId={}", firstCampaignId);

        logger.info("Generating campaign users...");

        // Генерируем базу для новых рассылок
        transactionTemplate.execute((status) -> {
            jdbcTemplate.update(
                    """
                    call gen_campaign_users(
                        p_start_campaign_id => ?::bigint,
                        p_campaigns_number => ?::int,
                        p_users_per_campaign => ?::int,
                        p_users_number => ?::int
                    )
                    """,
                    firstCampaignId,
                    CAMPAIGNS_NUMBER,
                    USERS_PER_CAMPAIGN,
                    USERS_NUMBER
            );
            return null;
        });

        logger.info("Campaign users has been generated");

        String runTimeStr = ZonedDateTime
                .now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MINUTES)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        logger.info("runTimeStr={}", runTimeStr);

        constantTags = List.of(
                new ImmutableTag("runTime", runTimeStr),
                new ImmutableTag("messageTableSize", String.valueOf(MESSAGE_TABLE_SIZE)),
                new ImmutableTag("threadsNumber", String.valueOf(WRITER_THREADS_NUMBER)),
                new ImmutableTag("maxBatchSize", String.valueOf(MAX_BATCH_SIZE)),
                new ImmutableTag("writer", MESSAGE_WRITER_TYPE.name()),
                new ImmutableTag("reader", MESSAGE_READER_TYPE.name())
        );

        Stream<Runnable> writerThreadRunnables = IntStream
                .range(0, WRITER_THREADS_NUMBER)
                .mapToObj(threadIndex -> {
                    int campaignIndexStart = threadIndex * CAMPAIGNS_PER_THREAD;
                    int campaignIndexEnd = Math.min(CAMPAIGNS_NUMBER, campaignIndexStart + CAMPAIGNS_PER_THREAD);
                    long campaignIdStart = firstCampaignId + campaignIndexStart;
                    long campaignIdEnd = firstCampaignId + campaignIndexEnd;
                    return () -> writerThread(campaignIdStart, campaignIdEnd);
                });

        Stream<Runnable> readerThreadRunnables = IntStream
                .range(0, READER_THREADS_NUMBER)
                .mapToObj(it -> this::readerThread);

        logger.info("Running threads");

        runThreads(Stream.concat(
                writerThreadRunnables,
                readerThreadRunnables
        ));
    }

    private void readerThread() {
        logger.info("start");
        while (true) {
            long userId = ThreadLocalRandom.current().nextLong(1, USERS_NUMBER + 1);

            List<Message> messages = listByUserMessagesTimerWrapper.withTimer(
                    () -> messageReader.listByUser(userId),
                    constantTags
            );

            //noinspection ResultOfMethodCallIgnored
            messages.size();

            if (Thread.interrupted()) return;
        }
    }

    private void writerThread(long campaignIdStart, long campaignIdEnd) {
        logger.info("start");
        for (long campaignId = campaignIdStart; campaignId < campaignIdEnd; campaignId++) {
            logger.info("campaignId={}", campaignId);
            writeCampaign(campaignId);
            if (Thread.interrupted()) return;
        }
    }

    private void writeCampaign(long campaignId) {
        logger.info("campaignId={}", campaignId);
        logger.info("Querying...");

        String topic = "Кампания " + campaignId;

        String text =
                Stream
                        .generate(() ->
                                "текст кампании " + campaignId
                        )
                        .limit(20)
                        .collect(Collectors.joining(", "));

        List<Long> userIds = jdbcTemplate.queryForList(
                """
                select
                    user_id
                from
                    campaign_users
                where
                    campaign_id = ?
                """,
                Long.class,
                campaignId
        );

        logger.info("Saving batches...");

        forEachBatch(userIds, MAX_BATCH_SIZE, (batchIndex, userIdsBatch) -> {
            logger.info("Running batchInsert: batchIndex={}, size={}", batchIndex, userIdsBatch.size());

            Instant created = Instant.now();

            List<Message> messagesBatch = userIdsBatch
                    .stream()
                    .map(userId -> {
                        Message message = new Message();
                        message.idUuid = UUID.randomUUID();
                        message.campaignId = campaignId;
                        message.userId = userId;
                        message.topic = topic;
                        message.text = text;
                        message.created = created;
                        message.sent = null;
                        message.deleted = null;
                        return message;
                    })
                    .toList();

            saveMessagesBatch(messagesBatch, MAX_BATCH_SIZE);
        });
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

    private void saveMessagesBatch(List<Message> messagesBatch, int maxBatchSize) {

        ArrayList<Tag> tags = new ArrayList<>(constantTags);

        boolean fullBatch = messagesBatch.size() == maxBatchSize;
        tags.add(new ImmutableTag("fullBatch", fullBatch ? "1" : "0"));

        batchInsertTimerWrapper.withTimer(
                () -> {
                    transactionTemplate.execute(status -> {
                        messageWriter.batchInsert(messagesBatch, messagesBatch.size());
                        return null;
                    });
                },
                tags
        );
    }

    private <T> void forEachBatch(List<T> list, int batchSize, BiConsumer<Integer, List<T>> batchConsumer) {
        int batchIndex = 0;
        for (int i = 0; i < list.size(); i += batchSize) {
            List<T> batch = list.subList(i, Math.min(i + batchSize, list.size()));
            batchConsumer.accept(batchIndex, batch);
            batchIndex++;
        }
    }
}
