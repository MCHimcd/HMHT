package ww.wangnong.web;

import java.util.List;
import java.util.Map;

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
