package ww.wangnong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.SensorMeta;

public interface SensorMetaRepository extends JpaRepository<SensorMeta, Long> {

    List<SensorMeta> findByDeviceId(String deviceId);

    Optional<SensorMeta> findByDeviceIdAndSensorKey(String deviceId, String sensorKey);
}
