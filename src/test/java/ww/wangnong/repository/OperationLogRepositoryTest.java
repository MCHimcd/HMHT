package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.OperationLog;

@DataJpaTest
class OperationLogRepositoryTest {

    @Autowired
    private OperationLogRepository repo;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void setUp() {
        em.persistAndFlush(TestDataFactory.operationLog("LOGIN", 1L));
        em.persistAndFlush(TestDataFactory.operationLog("LOGOUT", 1L));
        em.persistAndFlush(TestDataFactory.operationLog("LOGIN", 2L));
    }

    @Test
    void findByOrderByCreatedAtDesc_returnsPaged() {
        Page<OperationLog> page = repo.findByOrderByCreatedAtDesc(PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void findByOperationTypeOrderByCreatedAtDesc_filtersType() {
        Page<OperationLog> page = repo.findByOperationTypeOrderByCreatedAtDesc("LOGIN", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_filtersUserId() {
        Page<OperationLog> page = repo.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findTop50ByOrderByCreatedAtDesc_limitsResults() {
        List<OperationLog> logs = repo.findTop50ByOrderByCreatedAtDesc();

        assertThat(logs).hasSize(3);
    }
}
