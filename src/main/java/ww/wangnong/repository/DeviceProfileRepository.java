package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.DeviceProfile;

public interface DeviceProfileRepository extends JpaRepository<DeviceProfile, Long> {

    Optional<DeviceProfile> findByDeviceId(String deviceId);
}
