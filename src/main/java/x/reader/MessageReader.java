package x.reader;

import x.Message;

import java.util.List;

public interface MessageReader {

    MessageReaderType getType();

    List<Message> listByUser(Long userId);

    default Message findByIdBigint(Long idBigint) {
        throw new RuntimeException("Not implemented");
    }
}
