package x;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
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
        message.created     = fromDb(rs.getTimestamp(7));
        message.sent        = fromDb(rs.getTimestamp(8));
        message.deleted     = fromDb(rs.getTimestamp(9));
        //@formatter:on
        return message;
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void batchInsert(List<Message> messages, int batchSize) {
        jdbcTemplate.batchUpdate(
                """
                insert into
                    message(id_bigint, id_uuid, campaign_id, user_id, topic, text, created, sent, deleted)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                messages,
                batchSize,
                (ps, message) -> {
                    //@formatter:off
                    ps.setLong      (1, message.id_bigint     );
                    ps.setObject    (2, message.id_uuid       );
                    ps.setLong      (3, message.campaign_id   );
                    ps.setLong      (4, message.user_id       );
                    ps.setString    (5, message.topic         );
                    ps.setString    (6, message.text          );
                    ps.setTimestamp (7, toDb(message.created) );
                    ps.setTimestamp (8, toDb(message.sent)    );
                    ps.setTimestamp (9, toDb(message.deleted) );
                    //@formatter:on
                }
        );
    }
    
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

    private Timestamp toDb(@Nullable Instant instant) {
        if (instant == null) {
            return null;
        } else {
            return Timestamp.from(instant);
        }
    }

    private Instant fromDb(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        } else {
            return timestamp.toInstant();
        }
    }
}
