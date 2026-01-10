package x.reader;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import x.timer.AbstractTimerWrapper;

@Component
public class ReadMessagesTimerWrapper extends AbstractTimerWrapper {

    public ReadMessagesTimerWrapper(MeterRegistry registry) {
        super(registry, "app.db.message.read", "Получение сообщений пользователя из базы данных");
    }
}
