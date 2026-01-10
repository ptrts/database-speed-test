package x.reader;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import x.Message;

import java.util.List;

@Repository
public interface JpaMessageReader extends JpaRepository<Message, Long>, MessageReader  {

    List<Message> findByUserIdOrderByCreated(Long userId);

    default List<Message> listByUser(Long userId) {
        return findByUserIdOrderByCreated(userId);
    }
}
