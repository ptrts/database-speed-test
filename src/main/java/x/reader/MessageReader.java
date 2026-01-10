package x.reader;

import x.Message;

import java.util.List;

public interface MessageReader {

    MessageReaderType getType();

    List<Message> listByUser(Long userId);

    Message findByIdBigint(Long idBigint);
}
