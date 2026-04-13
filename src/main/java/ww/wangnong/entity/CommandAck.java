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
 * Purpose: 记录设备对控制命令的 ACK 回执流水。
 * Responsibilities: 保存 commandId、状态码、回执消息、traceId 和原始 payload，支撑命令闭环追踪。
 * Flow: 硬件 ACK 到达 -> ProtocolPersistenceService 写入 -> CommandAckTracker/查询接口按
 * commandId 关联。
 * Dependencies: command_ack 表、CommandRequest 主表。
 * Input: /hmht/device/{deviceId}/ack 主题解析结果。
 * Output: 命令回执审计记录。
 */
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
