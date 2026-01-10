package x.writer;

import x.Message;

import java.util.List;

public interface MessageWriter {

    MessageWriterType getType();

    void batchInsert(List<Message> messages, int batchSize);
}
