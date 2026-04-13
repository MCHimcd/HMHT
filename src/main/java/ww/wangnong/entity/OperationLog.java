package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 保存后台业务操作审计日志。
 * Responsibilities: 记录操作人、操作类型、目标对象、明细和时间，支撑后台审计查询。
 * Flow: 各业务服务调用 OperationLogService.log -> 持久化到 operation_logs -> 管理端分页查询。
 * Dependencies: operation_logs 表、OperationLogService。
 * Input: 业务操作元数据。
 * Output: 审计日志记录。
 */
@Entity
@Table(name = "operation_logs")
@Getter
@Setter
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    /**
     * LOGIN, LOGOUT, DEVICE_CONTROL, ALERT_HANDLE, THRESHOLD_UPDATE,
     * TASK_CREATE, TASK_UPDATE, TASK_DELETE, ROLE_CHANGE,
     * NODE_CREATE, NODE_UPDATE, NODE_DELETE
     */
    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
