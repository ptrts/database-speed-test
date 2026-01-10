package x.writer;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import x.timer.AbstractTimerWrapper;

@Component
public class BatchInsertTimerWrapper extends AbstractTimerWrapper {

    public BatchInsertTimerWrapper(MeterRegistry registry) {
        super(registry, "app.db.message.batchInsert", "Вставка батча сообщений в базу данных");
    }
}
