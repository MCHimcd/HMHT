package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.entity.CommandRetryLog;

@DataJpaTest
class CommandRetryLogRepositoryTest {

    @Autowired
    private CommandRetryLogRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void countByCommandId_existingLogs_returnsCount() {
        CommandRetryLog first = new CommandRetryLog();
        first.setCommandId("CMD-001");
        first.setAttemptNo(1);
        first.setRetryReason("timeout_exceeded");
        first.setRetryAt(LocalDateTime.of(2026, 4, 6, 10, 0));
        em.persistAndFlush(first);

        CommandRetryLog second = new CommandRetryLog();
        second.setCommandId("CMD-001");
        second.setAttemptNo(2);
        second.setRetryReason("timeout_exceeded");
        second.setRetryAt(LocalDateTime.of(2026, 4, 6, 10, 5));
        em.persistAndFlush(second);

        assertThat(repository.countByCommandId("CMD-001")).isEqualTo(2);
    }

    @Test
    void findByCommandIdOrderByCreatedAtDesc_existingLogs_returnsLogs() {
        CommandRetryLog log = new CommandRetryLog();
        log.setCommandId("CMD-002");
        log.setAttemptNo(1);
        log.setRetryReason("timeout_exceeded");
        log.setRetryAt(LocalDateTime.of(2026, 4, 6, 11, 0));
        em.persistAndFlush(log);

        var results = repository.findByCommandIdOrderByCreatedAtDesc("CMD-002");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getRetryReason()).isEqualTo("timeout_exceeded");
    }
}
