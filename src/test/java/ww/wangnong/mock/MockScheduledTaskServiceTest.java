package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.entity.ScheduledTask;
import ww.wangnong.entity.User;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.ScheduledTaskRepository;
import ww.wangnong.service.OperationLogService;
import ww.wangnong.service.ScheduledTaskService.TaskInfo;

@ExtendWith(MockitoExtension.class)
class MockScheduledTaskServiceTest {

    @Mock
    private MockAuthService authService;
    @Mock
    private ScheduledTaskRepository taskRepo;
    @Mock
    private OperationLogService opLog;

    private MockScheduledTaskService service;

    @BeforeEach
    void setUp() {
        service = new MockScheduledTaskService(authService, taskRepo, opLog);
    }

    private User stubUser() {
        User u = new User();
        u.setId(1L);
        u.setUserName("测试用户");
        u.setRole("user");
        return u;
    }

    private ScheduledTask stubTask(Long id, Long userId) {
        ScheduledTask t = new ScheduledTask();
        t.setId(id);
        t.setNodeId("N01");
        t.setUserId(userId);
        t.setEquipmentKey("water_pump");
        t.setAction("turn_on");
        t.setTaskName("定时灌溉");
        t.setHour(8);
        t.setMinute(0);
        t.setRepeatDays("1,2,3,4,5");
        t.setEnabled(true);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    // ── listByUser ──

    @Test
    void listByUser_returnsUserTasks() {
        User user = stubUser();
        when(authService.requireUser("token")).thenReturn(user);
        when(taskRepo.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(stubTask(1L, 1L)));

        List<TaskInfo> result = service.listByUser("token");

        assertThat(result).hasSize(1);
    }

    // ── create ──

    @Test
    void create_savesAndReturnsTask() {
        User user = stubUser();
        when(authService.requireUser("token")).thenReturn(user);

        ScheduledTask saved = stubTask(10L, 1L);
        when(taskRepo.save(any(ScheduledTask.class))).thenReturn(saved);

        TaskInfo result = service.create("token", "N01", "water_pump", "turn_on",
                "灌溉", 8, 0, "1,2,3");

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.equipmentKey()).isEqualTo("water_pump");
    }

    // ── delete ──

    @Test
    void delete_ownTask_deletesIt() {
        User user = stubUser();
        when(authService.requireUser("token")).thenReturn(user);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(stubTask(1L, 1L)));

        service.delete("token", 1L);

        verify(taskRepo).delete(any(ScheduledTask.class));
    }

    @Test
    void delete_otherUserTask_throwsBizException403() {
        User user = stubUser();
        when(authService.requireUser("token")).thenReturn(user);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(stubTask(1L, 99L)));

        assertThatThrownBy(() -> service.delete("token", 1L))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(403);
    }

    // ── toggleEnabled ──

    @Test
    void toggleEnabled_flipsState() {
        User user = stubUser();
        when(authService.requireUser("token")).thenReturn(user);
        ScheduledTask task = stubTask(1L, 1L);
        task.setEnabled(true);
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

        service.toggleEnabled("token", 1L);

        assertThat(task.getEnabled()).isFalse();
        verify(taskRepo).save(task);
    }
}
