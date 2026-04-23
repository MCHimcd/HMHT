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
 * Purpose: 持久化节点告警记录。
 * Responsibilities: 保存告警标识、来源、严重级别、处理状态和发生/处理时间，供告警列表与处理流程使用。
 * Flow: AlertService 创建并写入 -> Controller 查询 -> 告警处理后更新 handled 字段。
 * Dependencies: alerts 表、AlertService。
 * Input: 告警主题翻译结果或系统生成的告警信息。
 * Output: 告警历史与待处理队列数据。
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, unique = true, length = 50)
    private String alertId;

    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "type_name", length = 100)
    private String typeName;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 50)
    private String source;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "is_handled", nullable = false)
    private Boolean isHandled = false;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
