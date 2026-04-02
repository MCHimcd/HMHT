package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.User;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository repo;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByPhone_existingUser_returnsUser() {
        User user = TestDataFactory.user("13800000001", "测试用户");
        em.persistAndFlush(user);

        Optional<User> found = repo.findByPhone("13800000001");

        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("测试用户");
    }

    @Test
    void findByPhone_nonExistent_returnsEmpty() {
        Optional<User> found = repo.findByPhone("99999999999");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByPhone_existingPhone_returnsTrue() {
        User user = TestDataFactory.user("13800000002", "用户2");
        em.persistAndFlush(user);

        assertThat(repo.existsByPhone("13800000002")).isTrue();
    }

    @Test
    void existsByPhone_nonExistent_returnsFalse() {
        assertThat(repo.existsByPhone("99999999999")).isFalse();
    }
}
