package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 持久化设备 capability 快照中的设备控制项明细。
 * Responsibilities: 记录某个 deviceId 在某个 capabilityVersion 下的
 * equipmentKey、可控性与动作列表。
 * Flow: MqttBridge 收到 capability 主题 -> ProtocolPersistenceService 写入 ->
 * DeviceService/NodeService 查询聚合。
 * Dependencies: capability_equipment 表、DeviceCapability 主快照。
 * Input: 硬件 capability payload 中的 equipments 节点。
 * Output: 设备控制能力明细。
 */
@Entity
@Table(name = "capability_equipment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cap_equipment", columnNames = { "device_id", "capability_version",
                "equipment_key" })
})
@Getter
@Setter
public class CapabilityEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "capability_version", nullable = false, length = 20)
    private String capabilityVersion;

    @Column(name = "equipment_key", nullable = false, length = 80)
    private String equipmentKey;

    @Column(nullable = false)
    private Boolean controllable = true;

    @Column(name = "actions_json", columnDefinition = "TEXT")
    private String actionsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
