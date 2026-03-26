package ww.wangnong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.Node;

public interface NodeRepository extends JpaRepository<Node, Long> {

    Optional<Node> findByNodeId(String nodeId);

    List<Node> findByUserId(Long userId);

    List<Node> findAllByNodeIdIn(List<String> nodeIds);
}
