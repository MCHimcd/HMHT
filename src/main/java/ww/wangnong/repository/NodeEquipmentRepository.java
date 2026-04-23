package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import ww.wangnong.entity.NodeEquipment;

/**
 * Purpose: 提供节点设备静态绑定查询与清理能力。
 * Responsibilities: 按 nodeId 读取设备绑定，并在节点删除时批量清理。
 * Flow: 课程版种子/管理逻辑写入；NodeService 与节点删除流程读取或删除。
 * Dependencies: NodeEquipment 实体。
 * Output: 节点设备绑定列表。
 */
public interface NodeEquipmentRepository extends JpaRepository<NodeEquipment, Long> {

    List<NodeEquipment> findByNodeId(String nodeId);

    @Transactional
    void deleteByNodeId(String nodeId);
}
