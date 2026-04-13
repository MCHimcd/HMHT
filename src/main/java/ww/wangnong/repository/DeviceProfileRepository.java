package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.DeviceProfile;

/**
 * Purpose: 提供设备档案查询。
 * Responsibilities: 按 deviceId 读取唯一设备档案。
 * Flow: 桥接持久化更新 profile；NodeService 读取在线状态与节点映射。
 * Dependencies: DeviceProfile 实体。
 * Output: 设备档案记录。
 */
public interface DeviceProfileRepository extends JpaRepository<DeviceProfile, Long> {

    Optional<DeviceProfile> findByDeviceId(String deviceId);
}
