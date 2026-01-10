package x;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.time.Instant;
import java.util.UUID;


@Entity
public class Message {

    @Id
    @SequenceGenerator(name = "message_id_bigint_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_bigint_seq")
    @org.hibernate.annotations.ColumnDefault("nextval('message_id_bigint_seq')")
    public Long idBigint;

    //@Id
    //@GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    public UUID idUuid;

    @Column(nullable = false)
    public Long userId;

    @Column(nullable = false)
    public Long campaignId;

    @Column(nullable = false, length = 128)
    public String topic;

    @Column(nullable = false, length = 1024)
    public String text;

    public Instant created;
    public Instant sent;
    public Instant deleted;
}
