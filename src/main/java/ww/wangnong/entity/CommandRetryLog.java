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

/**
 * Purpose: 记录命令超时重试轨迹。
 * Responsibilities: 保存 commandId 每次重试的序号、原因与时间点，供诊断 ACK 超时链路使用。
 * Flow: CommandAckTracker 判断超时 -> 写入一条 retry log -> 继续发起下一次命令重试。
 * Dependencies: command_retry_log 表、CommandRequest 主表。
 * Input: 重试序号、原因和重试时间。
 * Output: 可审计的命令重试历史。
 */
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
