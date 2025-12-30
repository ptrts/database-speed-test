package x;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.stream.Stream;

// todo Может быть нужно добавить больше потоков в контейнер с PG и настроить больше воркеров в самом PG.
@SpringBootTest
public class BigTest {

    @Autowired
    private MessageJdbcRepository messageJdbcRepository;

    @Test
    public void test() {
        runThreads(Map.of(
                this::readerThread, 5
        ));
    }

    private void readerThread() {
        while (true) {
            messageJdbcRepository.getUserMessages(55L);
            if (Thread.interrupted()) return;
        }
    }

    private void runThreads(Map<Runnable, Integer> runnableToNumber) {
        Stream<Runnable> runnables = runnableToNumber
                .entrySet()
                .stream()
                .flatMap(entry ->
                        Stream
                                .generate(entry::getKey)
                                .limit(entry.getValue())
                );
        runThreads(runnables);
    }

    private void runThreads(Stream<Runnable> runnables) {
        runnables
                .map(Thread::new)
                .peek(Thread::start)
                .toList()
                .forEach(it -> {
                    try {
                        it.join();
                    } catch (InterruptedException ignored) {
                    }
                })
        ;
    }
}
