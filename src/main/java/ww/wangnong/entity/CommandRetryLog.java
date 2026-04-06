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
@Table(name = "command_retry_log", indexes = {
        @Index(name = "idx_cmd_retry_command", columnList = "command_id,created_at")
})
@Getter
@Setter
public class CommandRetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", nullable = false, length = 80)
    private String commandId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "retry_reason", length = 255)
    private String retryReason;

    @Column(name = "retry_at")
    private LocalDateTime retryAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
