package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;

@DataJpaTest
class UserNodeAccessRepositoryTest {

    @Autowired
    private UserNodeAccessRepository repo;

    @Autowired
    private TestEntityManager em;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.user("13800000001", "用户1");
        em.persistAndFlush(user);
    }

    @Test
    void findByUserId_returnsAccessList() {
        em.persistAndFlush(TestDataFactory.userNodeAccess(user.getId(), "N01"));
        em.persistAndFlush(TestDataFactory.userNodeAccess(user.getId(), "N02"));

        List<UserNodeAccess> result = repo.findByUserId(user.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findByNodeId_returnsAccessList() {
        User other = TestDataFactory.user("13800000002", "用户2");
        em.persistAndFlush(other);

        em.persistAndFlush(TestDataFactory.userNodeAccess(user.getId(), "N01"));
        em.persistAndFlush(TestDataFactory.userNodeAccess(other.getId(), "N01"));

        List<UserNodeAccess> result = repo.findByNodeId("N01");

        assertThat(result).hasSize(2);
    }

    @Test
    void existsByUserIdAndNodeId_returnsCorrectResult() {
        em.persistAndFlush(TestDataFactory.userNodeAccess(user.getId(), "N01"));

        assertThat(repo.existsByUserIdAndNodeId(user.getId(), "N01")).isTrue();
        assertThat(repo.existsByUserIdAndNodeId(user.getId(), "N99")).isFalse();
    }

    @Test
    void deleteByUserId_removesAllUserAccess() {
        em.persistAndFlush(TestDataFactory.userNodeAccess(user.getId(), "N01"));
        em.persistAndFlush(TestDataFactory.userNodeAccess(user.getId(), "N02"));

        repo.deleteByUserId(user.getId());
        em.flush();

        assertThat(repo.findByUserId(user.getId())).isEmpty();
    }

    @Test
    void deleteByUserIdAndNodeId_removesSingleAccess() {
        em.persistAndFlush(TestDataFactory.userNodeAccess(user.getId(), "N01"));
        em.persistAndFlush(TestDataFactory.userNodeAccess(user.getId(), "N02"));

        repo.deleteByUserIdAndNodeId(user.getId(), "N01");
        em.flush();

        List<UserNodeAccess> remaining = repo.findByUserId(user.getId());
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getNodeId()).isEqualTo("N02");
    }
}
