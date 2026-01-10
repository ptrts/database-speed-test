package x.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import x.DbConverter;
import x.Message;

import java.util.List;

@Component
public class TraditionalMessageBatchInserter implements MessageBatchInserter {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void batchInsert(List<Message> messages, int batchSize) {
        jdbcTemplate.batchUpdate(
                """
                insert into
                    message(id_uuid, campaign_id, user_id, topic, text, created, sent, deleted)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                messages,
                batchSize,
                (ps, message) -> {
                    //@formatter:off
                    ps.setObject    (1, message.idUuid                    );
                    ps.setLong      (2, message.campaignId                );
                    ps.setLong      (3, message.userId                    );
                    ps.setString    (4, message.topic                     );
                    ps.setString    (5, message.text                      );
                    ps.setTimestamp (6, DbConverter.toDb(message.created) );
                    ps.setTimestamp (7, DbConverter.toDb(message.sent)    );
                    ps.setTimestamp (8, DbConverter.toDb(message.deleted) );
                    //@formatter:on
                }
        );
    }
}
