package x.writer;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MessageWriterSelector {

    @Autowired
    private List<MessageWriter> messageWriters;

    private Map<MessageWriterType, MessageWriter> map;

    @PostConstruct
    public void postConstruct() {
        map = messageWriters
                .stream()
                .collect(Collectors.toMap(
                        MessageWriter::getType,
                        x -> x
                ));
    }

    public MessageWriter get(MessageWriterType type) {
        MessageWriter messageWriter = map.get(type);
        if (messageWriter == null) {
            throw new RuntimeException("Unsupported type " + type.name());
        }
        return messageWriter;
    }
}
