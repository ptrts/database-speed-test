package x.reader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import x.DbConverter;
import x.Message;

import java.util.List;
import java.util.UUID;

@Component
public class JdbcMessageReader implements MessageReader {

    private final String SELECT_FROM =
            """
            select
                id_bigint,
                id_uuid,
                campaign_id,
                user_id,
                topic,
                text,
                created,
                sent,
                deleted
            from
                message
            """;

    private final RowMapper<Message> ROW_MAPPER = (rs, rowNum) -> {
        Message message = new Message();
        //@formatter:off
        message.idBigint   = rs.getLong    (1);
        message.idUuid     = rs.getObject  (2, UUID.class);
        message.campaignId = rs.getLong    (3);
        message.userId     = rs.getLong    (4);
        message.topic       = rs.getString (5);
        message.text        = rs.getString (6);
        message.created     = DbConverter.fromDb(rs.getTimestamp(7));
        message.sent        = DbConverter.fromDb(rs.getTimestamp(8));
        message.deleted     = DbConverter.fromDb(rs.getTimestamp(9));
        //@formatter:on
        return message;
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public ReaderImplementation getType() {
        return ReaderImplementation.JDBC;
    }

    @SuppressWarnings("UnusedReturnValue")
    @Override
    public List<Message> listByUser(Long userId) {
        return jdbcTemplate.query(
                SELECT_FROM +
                """
                where
                    user_id = ?
                order by
                    created
                """,
                ROW_MAPPER,
                userId
        );
    }

    @Override
    public Message findByIdBigint(Long idBigint) {
        return jdbcTemplate.queryForObject(
                SELECT_FROM +
                """
                where
                    id_bigint = ?
                """,
                ROW_MAPPER,
                idBigint
        );
    }
}
