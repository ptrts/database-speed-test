package x.timer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.Callable;

public abstract class AbstractTimerWrapper {

    private final MeterRegistry registry;

    private final Meter.MeterProvider<Timer> timerProvider;

    public AbstractTimerWrapper(MeterRegistry registry, String name, String description) {
        this.registry = registry;
        this.timerProvider = Timer
                .builder(name)
                .description(description)
                .publishPercentiles(0.95, 0.99)
                .withRegistry(registry)
        ;
    }

    public <T> T withTimer(Callable<T> work, String... tags) {
        Timer.Sample sample = Timer.start(registry);
        try {
            try {
                return work.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            sample.stop(timerProvider.withTags(tags));
        }
    }

    public void withTimer(Runnable work, String... tags) {
        Timer.Sample sample = Timer.start(registry);
        try {
            work.run();
        } finally {
            sample.stop(timerProvider.withTags(tags));
        }
    }

    public void withTimer(Runnable work, Iterable<Tag> tags) {
        Timer.Sample sample = Timer.start(registry);
        try {
            work.run();
        } finally {
            sample.stop(timerProvider.withTags(tags));
        }
    }

    public <T> T withTimer(Callable<T> work, Iterable<Tag> tags) {
        Timer.Sample sample = Timer.start(registry);
        try {
            try {
                return work.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            sample.stop(timerProvider.withTags(tags));
        }
    }
}
