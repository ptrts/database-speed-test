package x.reader;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import x.timer.AbstractTimerWrapper;

@Component
public class ListByUserMessagesTimerWrapper extends AbstractTimerWrapper {

    public ListByUserMessagesTimerWrapper(MeterRegistry registry) {
        super(registry, "app.db.message.listByUser", "Получение списка сообщений пользователя из базы данных");
    }
}
