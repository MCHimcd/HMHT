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
@Table(name = "command_ack", indexes = {
        @Index(name = "idx_cmd_ack_command_id", columnList = "command_id"),
        @Index(name = "idx_cmd_ack_device_created", columnList = "device_id,created_at")
})
@Getter
@Setter
public class CommandAck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", nullable = false, length = 80)
    private String commandId;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "ack_code")
    private Integer ackCode;

    @Column(name = "ack_message", length = 255)
    private String ackMessage;

    @Column(name = "trace_id", length = 80)
    private String traceId;

    @Column(name = "source_topic", length = 200)
    private String sourceTopic;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "acked_at")
    private LocalDateTime ackedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
