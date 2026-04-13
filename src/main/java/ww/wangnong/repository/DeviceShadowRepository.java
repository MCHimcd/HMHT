package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.DeviceShadow;

/**
 * Purpose: 提供设备影子状态查询。
 * Responsibilities: 按 deviceId 读取唯一 shadow 记录。
 * Flow: 桥接更新 shadow；DeviceService 查询并解析 JSON。
 * Dependencies: DeviceShadow 实体。
 * Output: 设备最新影子状态。
 */
public interface DeviceShadowRepository extends JpaRepository<DeviceShadow, Long> {

    Optional<DeviceShadow> findByDeviceId(String deviceId);
}
