package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.DeviceCapability;

/**
 * Purpose: 提供设备 capability 主快照查询。
 * Responsibilities: 按 deviceId 读取最新 capability 头信息。
 * Flow: capability 入库后持续追加快照；NodeService/DeviceService 始终读取最新一条。
 * Dependencies: DeviceCapability 实体。
 * Output: 最新设备能力快照。
 */
public interface DeviceCapabilityRepository extends JpaRepository<DeviceCapability, Long> {

    // Invariant: 控制面查询只关心最新快照，因此必须按 createdAt 倒序取首条。
    Optional<DeviceCapability> findTopByDeviceIdOrderByCreatedAtDesc(String deviceId);
}
