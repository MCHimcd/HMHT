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
 * Purpose: 持久化设备 capability 快照中的传感器能力明细。
 * Responsibilities: 记录传感器 key、数据类型、单位、取值范围和必填约束，供设备能力查询与节点元数据聚合使用。
 * Flow: capability 主题入库 -> NodeService/DeviceService 按版本读取 -> 返回给前端能力视图。
 * Dependencies: capability_sensor 表、DeviceCapability 主快照。
 * Input: capability payload 中的 sensors 节点。
 * Output: 传感器能力明细。
 */
@Entity
@Table(name = "capability_sensor", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cap_sensor", columnNames = { "device_id", "capability_version", "sensor_key" })
})
@Getter
@Setter
public class CapabilitySensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "capability_version", nullable = false, length = 20)
    private String capabilityVersion;

    @Column(name = "sensor_key", nullable = false, length = 80)
    private String sensorKey;

    @Column(name = "data_type", nullable = false, length = 30)
    private String dataType;

    @Column(length = 20)
    private String unit;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "required_field", nullable = false)
    private Boolean requiredField = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
