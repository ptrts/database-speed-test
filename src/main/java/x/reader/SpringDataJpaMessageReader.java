package x.reader;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import x.Message;

import java.util.List;

@Repository
public interface SpringDataJpaMessageReader extends JpaRepository<Message, Long>, MessageReader  {

    @Override
    default MessageReaderType getType() {
        return MessageReaderType.SPRING_DATA_JPA;
    }

    @Override
    default List<Message> listByUser(Long userId) {
        return findByUserIdOrderByCreated(userId);
    }

    List<Message> findByUserIdOrderByCreated(Long userId);
}
