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
 * Purpose: 保存设备 capability 快照头信息。
 * Responsibilities: 记录 deviceId 的能力版本、校验值和上报时间，并作为 capability 明细表的聚合锚点。
 * Flow: capability 主题入库时先写主快照，再写 CapabilitySensor/CapabilityEquipment 明细。
 * Dependencies: device_capability 表、capability_* 明细表。
 * Input: capability payload 的头部字段。
 * Output: 每个设备最近一次能力声明版本。
 */
@Entity
@Table(name = "device_capability", indexes = {
        @Index(name = "idx_device_capability_device", columnList = "device_id,created_at")
})
@Getter
@Setter
public class DeviceCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "capability_version", nullable = false, length = 20)
    private String capabilityVersion;

    @Column(length = 80)
    private String checksum;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
