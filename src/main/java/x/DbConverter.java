package x;

import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Instant;

public class DbConverter {

    public static Timestamp toDb(@Nullable Instant instant) {
        if (instant == null) {
            return null;
        } else {
            return Timestamp.from(instant);
        }
    }

    public static  Instant fromDb(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        } else {
            return timestamp.toInstant();
        }
    }
}
