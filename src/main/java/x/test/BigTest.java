package x.test;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import x.Message;
import x.reader.MessageReader;
import x.reader.MessageReaderSelector;
import x.reader.ReadMessagesTimerWrapper;
import x.test.parameters.DataParameters;
import x.test.parameters.LaunchParameters;
import x.writer.BatchInsertTimerWrapper;
import x.writer.MessageWriter;
import x.writer.MessageWriterSelector;

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

@Component
public class BigTest {

    public static final Logger logger = LoggerFactory.getLogger(BigTest.class);

    @Autowired
    private MeterRegistry meterRegistry;

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
    private EntityManager entityManager;

    @Autowired
    private BatchInsertTimerWrapper batchInsertTimerWrapper;

    @Autowired
    private ReadMessagesTimerWrapper readMessagesTimerWrapper;

    private DataParameters dataParameters;

    private List<Tag> constantTags;

    private MessageWriter messageWriter;

    private MessageReader messageReader;

    private Runnable readRunnable;

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

    public void test(LaunchParameters launch) {
        entityManager.clear();
        dataParameters = entityManager.find(DataParameters.class, 1);

        logger.info("meterRegistry.clear()...");
        meterRegistry.clear();
        logger.info("Done");

        logger.info("Choosing writer...");
        messageWriter = messageWriterSelector.get(launch.writer.type);
        logger.info("Done");

        logger.info("Choosing reader...");
        messageReader = messageReaderSelector.get(launch.reader.implementation);
        logger.info("Done");

        switch (launch.reader.method) {
            case ROW -> readRunnable = this::readRandomMessage;
            case LIST -> readRunnable = this::readRandomUserMessages;
            default -> throw new RuntimeException("Unexpected value " + launch.reader.method);
        }

        logger.info("rollback_messages...");
        transactionTemplate.execute((status) -> {
            jdbcTemplate.update(
                    """
                            call _rollback_messages(
                                p_campaigns_number => ?,
                                p_users_per_campaign => ?
                            )
                            """
                    ,
                    dataParameters.campaignsNumber,
                    dataParameters.usersPerCampaign
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
                            call _gen_campaign_users(
                                p_start_campaign_id => ?::bigint,
                                p_campaigns_number => ?::int,
                                p_users_per_campaign => ?::int,
                                p_users_number => ?::int
                            )
                            """,
                    firstCampaignId,
                    launch.writer.getCampaignsNumber(),
                    launch.writer.usersPerCampaign,
                    dataParameters.usersNumber
            );
            return null;
        });

        logger.info("Campaign users has been generated");

        String runTimeStr = ZonedDateTime
                .now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        logger.info("runTimeStr={}", runTimeStr);

        constantTags = List.of(
                new ImmutableTag("runTime", runTimeStr),
                new ImmutableTag("messageTableSize", String.valueOf(dataParameters.getMessageTableSize())),
                new ImmutableTag("threadsNumber", String.valueOf(launch.writer.threadsNumber)),
                new ImmutableTag("maxBatchSize", String.valueOf(launch.writer.maxBatchSize)),
                new ImmutableTag("writer", launch.writer.type.name()),
                new ImmutableTag("reader", launch.reader.implementation.name()),
                new ImmutableTag("readType", launch.reader.method.name())
        );

        Stream<Runnable> writerThreadRunnables = IntStream
                .range(0, launch.writer.threadsNumber)
                .mapToObj(threadIndex -> {
                    int campaignIndexStart = threadIndex * launch.writer.campaignsPerThread;
                    int campaignIndexEnd = Math.min(launch.writer.getCampaignsNumber(), campaignIndexStart + launch.writer.campaignsPerThread);
                    long campaignIdStart = firstCampaignId + campaignIndexStart;
                    long campaignIdEnd = firstCampaignId + campaignIndexEnd;
                    return () -> writerThread(campaignIdStart, campaignIdEnd, launch.writer.maxBatchSize);
                });

        Stream<Runnable> readerThreadRunnables = IntStream
                .range(0, launch.reader.threadsNumber)
                .mapToObj(it -> this::readerThread);

        logger.info("Running threads");

        List<Thread> writerThreads = launchThreads(writerThreadRunnables);
        List<Thread> readerThreads = launchThreads(readerThreadRunnables);

        joinThreads(writerThreads);

        readerThreads.forEach(Thread::interrupt);
        joinThreads(readerThreads);

        meterRegistry.clear();
    }

    private void readerThread() {
        logger.info("start");
        while (true) {
            readRunnable.run();
            if (Thread.interrupted()) return;
        }
    }

    private void readRandomUserMessages() {
        long userId = ThreadLocalRandom.current().nextLong(1, dataParameters.usersNumber + 1);

        @SuppressWarnings("unused")
        List<Message> messages = readMessagesTimerWrapper.withTimer(
                () -> messageReader.listByUser(userId),
                constantTags
        );
    }

    private void readRandomMessage() {
        long messageId = ThreadLocalRandom.current().nextLong(1, dataParameters.getMessageTableSize() + 1);

        @SuppressWarnings("unused")
        Message message = readMessagesTimerWrapper.withTimer(
                () -> messageReader.findByIdBigint(messageId),
                constantTags
        );
    }

    private void writerThread(long campaignIdStart, long campaignIdEnd, int maxBatchSize) {
        logger.info("start");
        for (long campaignId = campaignIdStart; campaignId < campaignIdEnd; campaignId++) {
            logger.info("campaignId={}", campaignId);
            writeCampaign(campaignId, maxBatchSize);
            if (Thread.interrupted()) return;
        }
    }

    private void writeCampaign(long campaignId, int maxBatchSize) {
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

        forEachBatch(userIds, maxBatchSize, (batchIndex, userIdsBatch) -> {
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

            saveMessagesBatch(messagesBatch, maxBatchSize);
        });
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

    private static List<Thread> launchThreads(Stream<Runnable> runnables) {
        return runnables
                .map(Thread::new)
                .peek(Thread::start)
                .toList();
    }

    private static void joinThreads(List<Thread> threads) {
        threads
                .forEach(it -> {
                    try {
                        it.join();
                    } catch (InterruptedException ignored) {
                    }
                });
    }
}
