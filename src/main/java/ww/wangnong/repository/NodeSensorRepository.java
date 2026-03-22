package ww.wangnong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.NodeSensor;

public interface NodeSensorRepository extends JpaRepository<NodeSensor, Long> {

    List<NodeSensor> findByNodeId(String nodeId);

    Optional<NodeSensor> findByNodeIdAndSensorKey(String nodeId, String sensorKey);
}
