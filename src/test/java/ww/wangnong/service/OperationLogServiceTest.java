package ww.wangnong.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.OperationLog;

@DataJpaTest
@Import(OperationLogService.class)
class OperationLogServiceTest {

    @Autowired
    private OperationLogService service;

    @Autowired
    private TestEntityManager em;

    @Test
    void log_savesEntry() {
        service.log("LOGIN", 1L, "用户1", "user", "1", "登录成功");

        List<OperationLog> all = em.getEntityManager()
                .createQuery("SELECT o FROM OperationLog o", OperationLog.class)
                .getResultList();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getOperationType()).isEqualTo("LOGIN");
    }

    @Test
    void list_noFilter_returnsAll() {
        em.persistAndFlush(TestDataFactory.operationLog("LOGIN", 1L));
        em.persistAndFlush(TestDataFactory.operationLog("LOGOUT", 1L));

        OperationLogService.LogPage page = service.list(1, 10, null);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).hasSize(2);
    }

    @Test
    void list_withFilter_returnsFiltered() {
        em.persistAndFlush(TestDataFactory.operationLog("LOGIN", 1L));
        em.persistAndFlush(TestDataFactory.operationLog("LOGOUT", 1L));

        OperationLogService.LogPage page = service.list(1, 10, "LOGIN");

        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    void listByUser_filtersUserId() {
        em.persistAndFlush(TestDataFactory.operationLog("LOGIN", 1L));
        em.persistAndFlush(TestDataFactory.operationLog("LOGIN", 2L));

        OperationLogService.LogPage page = service.listByUser(1L, 1, 10);

        assertThat(page.total()).isEqualTo(1);
    }
}
