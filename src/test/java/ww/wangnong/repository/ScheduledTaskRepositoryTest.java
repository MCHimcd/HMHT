package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.ScheduledTask;
import ww.wangnong.entity.User;

@DataJpaTest
class ScheduledTaskRepositoryTest {

    @Autowired
    private ScheduledTaskRepository repo;

    @Autowired
    private TestEntityManager em;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.user("13800000001", "用户1");
        em.persistAndFlush(user);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsUserTasks() {
        em.persistAndFlush(TestDataFactory.scheduledTask("N01", user.getId()));
        em.persistAndFlush(TestDataFactory.scheduledTask("N02", user.getId()));

        User other = TestDataFactory.user("13800000002", "其他");
        em.persistAndFlush(other);
        em.persistAndFlush(TestDataFactory.scheduledTask("N01", other.getId()));

        List<ScheduledTask> tasks = repo.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(tasks).hasSize(2);
    }

    @Test
    void findByEnabledTrue_returnsOnlyEnabled() {
        ScheduledTask enabled = TestDataFactory.scheduledTask("N01", user.getId());
        enabled.setEnabled(true);
        em.persistAndFlush(enabled);

        ScheduledTask disabled = TestDataFactory.scheduledTask("N02", user.getId());
        disabled.setEnabled(false);
        em.persistAndFlush(disabled);

        List<ScheduledTask> result = repo.findByEnabledTrue();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeId()).isEqualTo("N01");
    }

    @Test
    void findByNodeId_returnsNodeTasks() {
        em.persistAndFlush(TestDataFactory.scheduledTask("N01", user.getId()));
        em.persistAndFlush(TestDataFactory.scheduledTask("N01", user.getId()));
        em.persistAndFlush(TestDataFactory.scheduledTask("N02", user.getId()));

        List<ScheduledTask> tasks = repo.findByNodeId("N01");

        assertThat(tasks).hasSize(2);
    }
}
