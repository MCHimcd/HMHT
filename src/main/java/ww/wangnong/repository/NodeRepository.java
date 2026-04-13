package ww.wangnong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.Node;

/**
 * Purpose: 提供节点主档案查询。
 * Responsibilities: 支持按 nodeId、所属用户和节点集合批量读取节点。
 * Flow: NodeService、AdminService 和种子流程读取或写入节点。
 * Dependencies: Node 实体。
 * Output: 节点记录或节点列表。
 */
public interface NodeRepository extends JpaRepository<Node, Long> {

    Optional<Node> findByNodeId(String nodeId);

    List<Node> findByUserId(Long userId);

    List<Node> findAllByNodeIdIn(List<String> nodeIds);
}
