package ww.wangnong.web;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;

/**
 * Purpose: 提供课程版应用可用性检查接口。
 * Responsibilities: 返回应用名、课程版标识、固定模拟节点和当前服务时间。
 * Flow: GET /v1/health -> 组装 HealthData -> ApiResponse.ok。
 * Dependencies: Spring 配置注入。
 * Input: 无显式业务输入。
 * Output: 适合前端启动探活与课程演示环境展示的健康数据。
 */
@RestController
@RequestMapping("/v1")
public class HealthController {

    @Value("${spring.application.name:HMHT}")
    private String appName;

    @GetMapping("/health")
    public ApiResponse<HealthData> health() {
        HealthData data = new HealthData(
                true,
                appName,
                "course",
                "SIM_Node_01",
                true,
                OffsetDateTime.now().toString());
        return ApiResponse.ok(data);
    }

    public record HealthData(
            boolean available,
            String application,
            String edition,
            String nodeId,
            boolean dataSimulated,
            String time) {
    }
}
