package ww.wangnong.service;

import java.util.List;

public interface ScheduledTaskService {

    record TaskInfo(long id, String nodeId, String equipmentKey, String action,
            String taskName, int hour, int minute, String repeatDays,
            boolean enabled, long createdAt) {
    }

    List<TaskInfo> listByUser(String token);

    TaskInfo create(String token, String nodeId, String equipmentKey, String action,
            String taskName, int hour, int minute, String repeatDays);

    TaskInfo update(String token, long taskId, String nodeId, String equipmentKey,
            String action, String taskName, int hour, int minute, String repeatDays);

    void delete(String token, long taskId);

    void toggleEnabled(String token, long taskId);
}
