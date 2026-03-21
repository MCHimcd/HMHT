package ww.wangnong.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.AlertService;

@RestController
@RequestMapping("/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/list")
    public ApiResponse<?> listAlerts(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> query = body.containsKey("query")
                ? new HashMap<>((Map<String, Object>) body.get("query"))
                : new HashMap<>();

        if (!query.containsKey("pageSize") && query.containsKey("size")) {
            query.put("pageSize", query.get("size"));
        }

        return ApiResponse.ok(alertService.queryAlerts(query));
    }

    record HandleRequest(Map<String, Object> data) {}

    @PostMapping("/handle")
    public ApiResponse<?> handleAlert(@RequestBody HandleRequest req) {
        String alertId = String.valueOf(req.data().get("alert_id"));
        String action = String.valueOf(req.data().getOrDefault("action", "handle"));
        alertService.handleAlert(alertId, action);
        return ApiResponse.ok();
    }
}
