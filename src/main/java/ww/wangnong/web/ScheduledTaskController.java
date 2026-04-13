package ww.wangnong.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.ScheduledTaskService;

/**
 * Purpose: 暴露用户定时任务的查询与增删改停用接口。
 * Responsibilities: 将任务请求体映射为 service 参数，统一处理用户 token 提取，保持控制器层无排程业务逻辑。
 * Flow: HTTP 请求 -> extractToken -> ScheduledTaskService 执行 CRUD 或启停 ->
 * ApiResponse.ok。
 * Dependencies: ScheduledTaskService、ApiResponse。
 * Input: Bearer token、任务标识、设备动作与重复周期字段。
 * Output: 任务列表、创建/更新结果或空成功响应。
 * Side Effects: 变更数据库中的任务定义及启用状态。
 */
@RestController
@RequestMapping("/v1/tasks")
public class ScheduledTaskController {

    private final ScheduledTaskService taskService;

    public ScheduledTaskController(ScheduledTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ApiResponse<?> list(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
        return ApiResponse.ok(taskService.listByUser(extractToken(auth)));
    }

    record CreateRequest(String nodeId, String equipmentKey, String action,
            String taskName, int hour, int minute, String repeatDays) {
    }

    @PostMapping
    public ApiResponse<?> create(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @RequestBody CreateRequest req) {
        return ApiResponse.ok(taskService.create(extractToken(auth),
                req.nodeId(), req.equipmentKey(), req.action(),
                req.taskName(), req.hour(), req.minute(), req.repeatDays()));
    }

    record UpdateRequest(String nodeId, String equipmentKey, String action,
            String taskName, int hour, int minute, String repeatDays) {
    }

    @PutMapping("/{taskId}")
    public ApiResponse<?> update(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable long taskId,
            @RequestBody UpdateRequest req) {
        return ApiResponse.ok(taskService.update(extractToken(auth), taskId,
                req.nodeId(), req.equipmentKey(), req.action(),
                req.taskName(), req.hour(), req.minute(), req.repeatDays()));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<?> delete(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable long taskId) {
        taskService.delete(extractToken(auth), taskId);
        return ApiResponse.ok();
    }

    @PutMapping("/{taskId}/toggle")
    public ApiResponse<?> toggle(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable long taskId) {
        taskService.toggleEnabled(extractToken(auth), taskId);
        return ApiResponse.ok();
    }

    private String extractToken(String auth) {
        return auth.startsWith("Bearer ") ? auth.substring(7) : auth;
    }
}
