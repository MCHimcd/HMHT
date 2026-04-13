package ww.wangnong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import ww.wangnong.entity.NodeSensor;

/**
 * Purpose: 提供节点传感器绑定与阈值配置查询。
 * Responsibilities: 按 nodeId 或 nodeId+sensorKey 读取绑定，并支持节点级批量清理。
 * Flow: NodeService 更新阈值；MockDataLifecycle 或节点删除流程写入/删除。
 * Dependencies: NodeSensor 实体。
 * Output: 节点传感器绑定列表或单条绑定。
 */
public interface NodeSensorRepository extends JpaRepository<NodeSensor, Long> {

    List<NodeSensor> findByNodeId(String nodeId);

    Optional<NodeSensor> findByNodeIdAndSensorKey(String nodeId, String sensorKey);

    @Transactional
    void deleteByNodeId(String nodeId);
}
