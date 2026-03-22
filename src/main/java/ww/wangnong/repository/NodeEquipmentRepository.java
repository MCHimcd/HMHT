package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.NodeEquipment;

public interface NodeEquipmentRepository extends JpaRepository<NodeEquipment, Long> {

    List<NodeEquipment> findByNodeId(String nodeId);
}
