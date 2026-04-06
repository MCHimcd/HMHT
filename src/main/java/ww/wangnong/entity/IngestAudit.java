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
@Table(name = "ingest_audit", indexes = {
        @Index(name = "idx_ingest_audit_device_created", columnList = "device_id,created_at"),
        @Index(name = "idx_ingest_audit_type_created", columnList = "msg_type,created_at")
})
@Getter
@Setter
public class IngestAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "msg_type", nullable = false, length = 30)
    private String msgType;

    @Column(name = "source_topic", nullable = false, length = 200)
    private String sourceTopic;

    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Column(name = "protocol_version", length = 20)
    private String protocolVersion;

    @Column(name = "trace_id", length = 80)
    private String traceId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
