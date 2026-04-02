package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.Node;
import ww.wangnong.entity.User;

@DataJpaTest
class NodeRepositoryTest {

    @Autowired
    private NodeRepository repo;

    @Autowired
    private TestEntityManager em;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = TestDataFactory.user("13800000001", "节点主人");
        em.persistAndFlush(owner);
    }

    @Test
    void findByNodeId_existingNode_returnsNode() {
        Node node = TestDataFactory.node("N01", "温室1号", owner);
        em.persistAndFlush(node);

        Optional<Node> found = repo.findByNodeId("N01");

        assertThat(found).isPresent();
        assertThat(found.get().getNodeName()).isEqualTo("温室1号");
    }

    @Test
    void findByNodeId_nonExistent_returnsEmpty() {
        assertThat(repo.findByNodeId("NONEXISTENT")).isEmpty();
    }

    @Test
    void findByUserId_returnsOwnedNodes() {
        em.persistAndFlush(TestDataFactory.node("N01", "温室1", owner));
        em.persistAndFlush(TestDataFactory.node("N02", "温室2", owner));

        User other = TestDataFactory.user("13800000002", "其他人");
        em.persistAndFlush(other);
        em.persistAndFlush(TestDataFactory.node("N03", "温室3", other));

        List<Node> nodes = repo.findByUserId(owner.getId());

        assertThat(nodes).hasSize(2);
        assertThat(nodes).extracting(Node::getNodeId).containsExactlyInAnyOrder("N01", "N02");
    }

    @Test
    void findAllByNodeIdIn_returnsMatchingNodes() {
        em.persistAndFlush(TestDataFactory.node("N01", "温室1", owner));
        em.persistAndFlush(TestDataFactory.node("N02", "温室2", owner));
        em.persistAndFlush(TestDataFactory.node("N03", "温室3", owner));

        List<Node> nodes = repo.findAllByNodeIdIn(List.of("N01", "N03"));

        assertThat(nodes).hasSize(2);
        assertThat(nodes).extracting(Node::getNodeId).containsExactlyInAnyOrder("N01", "N03");
    }
}
