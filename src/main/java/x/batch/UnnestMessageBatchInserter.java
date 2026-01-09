package x.batch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import x.DbConverter;
import x.Message;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Component
public class UnnestMessageBatchInserter implements MessageBatchInserter {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void batchInsert(List<Message> messages, int batchSize) {

        int size = messages.size();

        //@formatter:off
        UUID    [] java_array_id_uuid     = new UUID    [size];
        Long    [] java_array_user_id     = new Long    [size];
        Long    [] java_array_campaign_id = new Long    [size];
        String  [] java_array_topic       = new String  [size];
        String  [] java_array_text        = new String  [size];
        Timestamp [] java_array_created   = new Timestamp [size];
        Timestamp [] java_array_sent      = new Timestamp [size];
        Timestamp [] java_array_deleted   = new Timestamp [size];
        //@formatter:on

        for (int i = 0; i < size; i++) {
            Message message = messages.get(i);

            //@formatter:off
            java_array_id_uuid     [i] = message.id_uuid;
            java_array_user_id     [i] = message.user_id;
            java_array_campaign_id [i] = message.campaign_id;
            java_array_topic       [i] = message.topic;
            java_array_text        [i] = message.text;
            java_array_created     [i] = DbConverter.toDb(message.created);
            java_array_sent        [i] = DbConverter.toDb(message.sent);
            java_array_deleted     [i] = DbConverter.toDb(message.deleted);
            //@formatter:on
        }

        jdbcTemplate.execute(
                """
                insert into
                    message(
                        id_uuid,
                        campaign_id,
                        user_id,
                        topic,
                        text,
                        created,
                        sent,
                        deleted
                    )
                    select
                        *
                    from
                        unnest(
                            ?::uuid[],
                            ?::bigint[],
                            ?::bigint[],
                            ?::varchar[],
                            ?::varchar[],
                            ?::timestamptz[],
                            ?::timestamptz[],
                            ?::timestamptz[]
                        )
                """,
                (PreparedStatement ps) -> {

                    Connection connection = ps.getConnection();

                    //@formatter:off
                    Array array_id_uuid     = connection.createArrayOf("uuid"        , java_array_id_uuid     );
                    Array array_user_id     = connection.createArrayOf("bigint"      , java_array_user_id     );
                    Array array_campaign_id = connection.createArrayOf("bigint"      , java_array_campaign_id );
                    Array array_topic       = connection.createArrayOf("varchar"     , java_array_topic       );
                    Array array_text        = connection.createArrayOf("varchar"     , java_array_text        );
                    Array array_created     = connection.createArrayOf("timestamptz" , java_array_created     );
                    Array array_sent        = connection.createArrayOf("timestamptz" , java_array_sent        );
                    Array array_deleted     = connection.createArrayOf("timestamptz" , java_array_deleted     );
                    //@formatter:on

                    //@formatter:off
                    ps.setArray(1, array_id_uuid);
                    ps.setArray(2, array_campaign_id);
                    ps.setArray(3, array_user_id);
                    ps.setArray(4, array_topic);
                    ps.setArray(5, array_text);
                    ps.setArray(6, array_created);
                    ps.setArray(7, array_sent);
                    ps.setArray(8, array_deleted);
                    //@formatter:on

                    return ps.executeUpdate();
                }
        );
    }

    public <T> Array createArrayOf(String sqlTypeName, T[] javaArray) {
        return jdbcTemplate.execute((Connection connection) ->
                connection.createArrayOf(sqlTypeName, javaArray)
        );
    }
}
