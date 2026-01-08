package x;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MessageJdbcRepository {

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
        message.id_bigint   = rs.getLong   (1);
        message.id_uuid     = rs.getObject (2, UUID.class);
        message.campaign_id = rs.getLong   (3);
        message.user_id     = rs.getLong   (4);
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

    public Message find(Long id_bigint) {
        return jdbcTemplate.queryForObject(
                SELECT_FROM +
                """
                where
                    id_bigint = ?
                """,
                ROW_MAPPER,
                id_bigint
        );
    }

    public List<Message> getUserMessages(Long user_id) {
        return jdbcTemplate.query(
                SELECT_FROM +
                """
                where
                    user_id = ?
                """,
                ROW_MAPPER,
                user_id
        );
    }
}
