package x;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

@Component
public class MessageJdbcRepository {

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
                    ps.setLong      (1, message.id_bigint               );
                    ps.setObject    (2, message.id_uuid                 );
                    ps.setLong      (3, message.campaign_id             );
                    ps.setLong      (4, message.user_id                 );
                    ps.setString    (5, message.topic                   );
                    ps.setString    (6, message.text                    );
                    ps.setTimestamp (7, Timestamp.from(message.created) );
                    ps.setTimestamp (8, Timestamp.from(message.sent)    );
                    ps.setTimestamp (9, Timestamp.from(message.deleted) );
                    //@formatter:on
                }
        );
    }
}
