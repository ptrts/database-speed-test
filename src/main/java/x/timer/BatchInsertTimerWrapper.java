package x.timer;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BatchInsertTimerWrapper extends AbstractTimerWrapper{

    public BatchInsertTimerWrapper(MeterRegistry registry) {
        super(registry, "app.db.message.batchInsert", "Запуск метода MessageJdbcRepository.batchInsert");
    }
}
