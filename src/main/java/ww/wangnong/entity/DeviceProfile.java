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
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 保存设备档案与在线心跳摘要。
 * Responsibilities: 记录设备与节点映射、展示名、软硬件版本、厂商和最近在线状态，供节点状态判断使用。
 * Flow: 桥接层接收硬件元信息或 telemetry -> 更新 profile -> NodeService 基于 online/lastSeenAt
 * 聚合在线状态。
 * Dependencies: device_profile 表、NodeService。
 * Input: 设备档案元数据与在线心跳信息。
 * Output: 设备档案快照。
 */
@Entity
@Table(name = "device_profile", indexes = {
        @Index(name = "idx_device_profile_node", columnList = "node_id")
})
@Getter
@Setter
public class DeviceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 50)
    private String deviceId;

    @Column(name = "node_id", length = 50)
    private String nodeId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "hardware_model", length = 100)
    private String hardwareModel;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(length = 100)
    private String vendor;

    @Column(nullable = false)
    private Boolean online = false;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
