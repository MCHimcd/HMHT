package ww.wangnong.mock;

import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;

import ww.wangnong.entity.ScheduledTask;
import ww.wangnong.entity.User;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.ScheduledTaskRepository;
import ww.wangnong.service.OperationLogService;
import ww.wangnong.service.ScheduledTaskService;

@Service
public class MockScheduledTaskService implements ScheduledTaskService {

    private final MockAuthService authService;
    private final ScheduledTaskRepository taskRepo;
    private final OperationLogService opLog;

    public MockScheduledTaskService(MockAuthService authService,
            ScheduledTaskRepository taskRepo,
            OperationLogService opLog) {
        this.authService = authService;
        this.taskRepo = taskRepo;
        this.opLog = opLog;
    }

    @Override
    public List<TaskInfo> listByUser(String token) {
        User user = authService.requireUser(token);
        return taskRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toInfo).toList();
    }

    @Override
    public TaskInfo create(String token, String nodeId, String equipmentKey, String action,
            String taskName, int hour, int minute, String repeatDays) {
        User user = authService.requireUser(token);
        ScheduledTask task = new ScheduledTask();
        task.setNodeId(nodeId);
        task.setUserId(user.getId());
        task.setEquipmentKey(equipmentKey);
        task.setAction(action);
        task.setTaskName(taskName);
        task.setHour(hour);
        task.setMinute(minute);
        task.setRepeatDays(repeatDays);
        task.setEnabled(true);
        task = taskRepo.save(task);

        opLog.log("TASK_CREATE", user.getId(), user.getUserName(),
                "scheduled_task", String.valueOf(task.getId()),
                taskName + " → " + equipmentKey + " " + action + " @" + hour + ":" + minute);
        return toInfo(task);
    }

    @Override
    public TaskInfo update(String token, long taskId, String nodeId, String equipmentKey,
            String action, String taskName, int hour, int minute, String repeatDays) {
        User user = authService.requireUser(token);
        ScheduledTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new BizException(404, "任务不存在"));
        if (!task.getUserId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new BizException(403, "无权操作");
        }
        task.setNodeId(nodeId);
        task.setEquipmentKey(equipmentKey);
        task.setAction(action);
        task.setTaskName(taskName);
        task.setHour(hour);
        task.setMinute(minute);
        task.setRepeatDays(repeatDays);
        task = taskRepo.save(task);

        opLog.log("TASK_UPDATE", user.getId(), user.getUserName(),
                "scheduled_task", String.valueOf(task.getId()),
                taskName + " → " + equipmentKey + " " + action);
        return toInfo(task);
    }

    @Override
    public void delete(String token, long taskId) {
        User user = authService.requireUser(token);
        ScheduledTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new BizException(404, "任务不存在"));
        if (!task.getUserId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new BizException(403, "无权操作");
        }
        taskRepo.delete(task);

        opLog.log("TASK_DELETE", user.getId(), user.getUserName(),
                "scheduled_task", String.valueOf(taskId), task.getTaskName());
    }

    @Override
    public void toggleEnabled(String token, long taskId) {
        User user = authService.requireUser(token);
        ScheduledTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new BizException(404, "任务不存在"));
        if (!task.getUserId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new BizException(403, "无权操作");
        }
        task.setEnabled(!task.getEnabled());
        taskRepo.save(task);

        opLog.log("TASK_UPDATE", user.getId(), user.getUserName(),
                "scheduled_task", String.valueOf(taskId),
                task.getTaskName() + " → " + (task.getEnabled() ? "启用" : "禁用"));
    }

    private TaskInfo toInfo(ScheduledTask t) {
        long createdMs = t.getCreatedAt() != null
                ? t.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : 0;
        return new TaskInfo(t.getId(), t.getNodeId(), t.getEquipmentKey(), t.getAction(),
                t.getTaskName(), t.getHour(), t.getMinute(), t.getRepeatDays(),
                t.getEnabled(), createdMs);
    }
}
