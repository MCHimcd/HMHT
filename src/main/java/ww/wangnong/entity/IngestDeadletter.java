package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ingest_deadletter", indexes = {
        @Index(name = "idx_ingest_deadletter_device_created", columnList = "device_id,created_at"),
        @Index(name = "idx_ingest_deadletter_type_created", columnList = "msg_type,created_at")
})
@Getter
@Setter
public class IngestDeadletter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "msg_type", nullable = false, length = 30)
    private String msgType;

    @Column(name = "source_topic", nullable = false, length = 200)
    private String sourceTopic;

    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Column(name = "trace_id", length = 80)
    private String traceId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
