package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.DeviceCapability;

public interface DeviceCapabilityRepository extends JpaRepository<DeviceCapability, Long> {

    Optional<DeviceCapability> findTopByDeviceIdOrderByCreatedAtDesc(String deviceId);
}
