package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 保存设备最新影子状态。
 * Responsibilities: 以 deviceId 为唯一键维护当前 shadow JSON，供控制面和前端直接读取设备当前状态。
 * Flow: telemetry/ack 更新 shadow -> DeviceService 查询并解析 shadowJson -> 返回给前端。
 * Dependencies: device_shadow 表、DeviceService。
 * Input: 设备最新状态 JSON。
 * Output: 每个设备唯一一条最新影子记录。
 * Invariant: 同一 deviceId 在表中只能存在一条 shadow 记录。
 */
@Entity
@Table(name = "device_shadow")
@Getter
@Setter
public class DeviceShadow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 50)
    private String deviceId;

    @Column(name = "shadow_json", columnDefinition = "TEXT")
    private String shadowJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
