package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.Alert;

@DataJpaTest
class AlertRepositoryTest {

    @Autowired
    private AlertRepository repo;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByAlertId_existingAlert_returnsAlert() {
        Alert alert = TestDataFactory.alert("ALT-001", "N01");
        em.persistAndFlush(alert);

        Optional<Alert> found = repo.findByAlertId("ALT-001");

        assertThat(found).isPresent();
        assertThat(found.get().getNodeId()).isEqualTo("N01");
    }

    @Test
    void findByAlertId_nonExistent_returnsEmpty() {
        assertThat(repo.findByAlertId("NONEXISTENT")).isEmpty();
    }

    @Test
    void findByNodeIdOrderByCreatedAtDesc_returnsPagedResults() {
        em.persistAndFlush(TestDataFactory.alert("ALT-001", "N01"));
        em.persistAndFlush(TestDataFactory.alert("ALT-002", "N01"));
        em.persistAndFlush(TestDataFactory.alert("ALT-003", "N02"));

        Page<Alert> page = repo.findByNodeIdOrderByCreatedAtDesc("N01", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Alert::getNodeId).containsOnly("N01");
    }

    @Test
    void findByIsHandledOrderByCreatedAtDesc_filtersHandled() {
        Alert handled = TestDataFactory.alert("ALT-H01", "N01");
        handled.setIsHandled(true);
        em.persistAndFlush(handled);

        Alert unhandled = TestDataFactory.alert("ALT-U01", "N01");
        em.persistAndFlush(unhandled);

        Page<Alert> handledPage = repo.findByIsHandledOrderByCreatedAtDesc(true, PageRequest.of(0, 10));
        Page<Alert> unhandledPage = repo.findByIsHandledOrderByCreatedAtDesc(false, PageRequest.of(0, 10));

        assertThat(handledPage.getTotalElements()).isEqualTo(1);
        assertThat(unhandledPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findTop10ByIsHandledOrderByCreatedAtDesc_limitsTo10() {
        for (int i = 0; i < 15; i++) {
            Alert a = TestDataFactory.alert("ALT-" + i, "N01");
            a.setIsHandled(true);
            em.persistAndFlush(a);
        }

        List<Alert> top10 = repo.findTop10ByIsHandledOrderByCreatedAtDesc(true);

        assertThat(top10).hasSize(10);
    }
}
