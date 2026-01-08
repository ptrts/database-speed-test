package x.batch;

import x.Message;

import java.util.List;

public interface MessageBatchInserter {
    void batchInsert(List<Message> messages, int batchSize);
}
