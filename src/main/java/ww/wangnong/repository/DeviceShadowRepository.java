package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.DeviceShadow;

public interface DeviceShadowRepository extends JpaRepository<DeviceShadow, Long> {

    Optional<DeviceShadow> findByDeviceId(String deviceId);
}
