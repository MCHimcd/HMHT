package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.SensorType;

public interface SensorTypeRepository extends JpaRepository<SensorType, Long> {

    Optional<SensorType> findBySensorKey(String sensorKey);
}
