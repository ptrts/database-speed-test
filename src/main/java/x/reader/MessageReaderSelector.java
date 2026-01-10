package x.reader;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MessageReaderSelector {

    @Autowired
    private List<MessageReader> messageReaders;

    private Map<MessageReaderType, MessageReader> map;

    @PostConstruct
    public void postConstruct() {
        map = messageReaders
                .stream()
                .collect(Collectors.toMap(
                        MessageReader::getType,
                        x -> x
                ));
    }

    public MessageReader get(MessageReaderType type) {
        MessageReader messageReader = map.get(type);
        if (messageReader == null) {
            throw new RuntimeException("Unsupported type " + type.name());
        }
        return messageReader;
    }
}
