package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 作为命令闭环的主记录表，描述一次控制命令的完整生命周期。
 * Responsibilities: 保存目标设备、动作、状态、超时配置、转发主题、原始 payload、重试计数和 ACK 时间。
 * Flow: App 命令入站 -> ProtocolPersistenceService 创建请求 -> ACK/重试流程持续更新状态与时间戳。
 * Dependencies: command_request 表、CommandAck、CommandRetryLog、CommandAckTracker。
 * Input: App 控制命令与后续重试/ACK 状态更新。
 * Output: 当前命令生命周期快照。
 * Invariant: command_id 全局唯一，retry_count 只允许沿生命周期单调增加。
 */
@Entity
@Table(name = "command_request", indexes = {
                @Index(name = "idx_cmd_req_device_created", columnList = "device_id,created_at"),
                @Index(name = "idx_cmd_req_status_created", columnList = "status,created_at")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uk_cmd_req_command_id", columnNames = { "command_id" })
})
@Getter
@Setter
public class CommandRequest {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "command_id", nullable = false, length = 80)
        private String commandId;

        @Column(name = "device_id", nullable = false, length = 50)
        private String deviceId;

        @Column(name = "target_key", nullable = false, length = 80)
        private String targetKey;

        @Column(nullable = false, length = 80)
        private String action;

        @Column(nullable = false, length = 30)
        private String status;

        @Column(name = "trace_id", length = 80)
        private String traceId;

        @Column(name = "timeout_ms")
        private Integer timeoutMs;

        @Column(name = "source_topic", length = 200)
        private String sourceTopic;

        @Column(name = "relay_topic", length = 200)
        private String relayTopic;

        @Column(name = "raw_payload", columnDefinition = "TEXT")
        private String rawPayload;

        @Column(name = "retry_count", nullable = false)
        private Integer retryCount = 0;

        @Column(name = "sent_at")
        private LocalDateTime sentAt;

        @Column(name = "acked_at")
        private LocalDateTime ackedAt;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
}
