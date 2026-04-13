package ww.wangnong.service;

import java.util.List;

/**
 * Purpose: 定义用户定时任务管理契约。
 * Responsibilities: 提供任务列表、创建、更新、删除和启停切换的统一入口。
 * Flow: 控制器传入 token 与任务参数 -> 实现类校验归属关系并读写任务仓库 -> 返回 TaskInfo 或空成功结果。
 * Dependencies: 定时任务实体与仓库、AuthService 身份校验。
 * Input: token、任务标识、节点/设备动作、重复周期。
 * Output: 任务详情列表或单条任务结果。
 */
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
