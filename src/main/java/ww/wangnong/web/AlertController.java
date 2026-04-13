package ww.wangnong.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.AlertService;

/**
 * Purpose: 暴露告警分页查询与处理接口。
 * Responsibilities: 兼容前端不同查询字段命名，转发告警处理动作，并保持控制器层只做轻量请求整形。
 * Flow: 接收 body -> 归一化 query 参数 -> AlertService 查询或处理 -> ApiResponse.ok。
 * Dependencies: AlertService、ApiResponse。
 * Input: 告警查询条件、告警处理动作。
 * Output: 告警分页结果或空成功响应。
 */
@RestController
@RequestMapping("/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/list")
    public ApiResponse<?> listAlerts(@RequestBody Map<String, Object> body) {
        // Reason: 前端历史上同时使用 size 与 pageSize，这里统一成 service 侧约定字段，避免查询层分叉兼容。
        @SuppressWarnings("unchecked")
        Map<String, Object> query = body.containsKey("query")
                ? new HashMap<>((Map<String, Object>) body.get("query"))
                : new HashMap<>();

        if (!query.containsKey("pageSize") && query.containsKey("size")) {
            query.put("pageSize", query.get("size"));
        }

        return ApiResponse.ok(alertService.queryAlerts(query));
    }

    record HandleRequest(Map<String, Object> data) {
    }

    @PostMapping("/handle")
    public ApiResponse<?> handleAlert(@RequestBody HandleRequest req) {
        String alertId = String.valueOf(req.data().get("alert_id"));
        String action = String.valueOf(req.data().getOrDefault("action", "handle"));
        alertService.handleAlert(alertId, action);
        return ApiResponse.ok();
    }
}
