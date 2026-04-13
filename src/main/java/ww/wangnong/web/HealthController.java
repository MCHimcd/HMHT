package ww.wangnong.web;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.mqtt.PahoMqttService;

/**
 * Purpose: 提供前端消费的应用可用性检查接口。
 * Responsibilities: 汇总应用名、模拟数据标记和 MQTT 连接状态，形成轻量健康快照。
 * Flow: GET /v1/health -> 读取配置与 MQTT 生命周期状态 -> 组装 HealthData -> ApiResponse.ok。
 * Dependencies: PahoMqttService、Spring 配置注入。
 * Input: 无显式业务输入。
 * Output: 适合前端启动探活与环境展示的健康数据。
 */
@RestController
@RequestMapping("/v1")
public class HealthController {

    private final PahoMqttService mqttService;

    @Value("${spring.application.name:HMHT}")
    private String appName;

    @Value("${hmht.data.simulated:true}")
    private boolean dataSimulated;

    public HealthController(PahoMqttService mqttService) {
        this.mqttService = mqttService;
    }

    @GetMapping("/health")
    public ApiResponse<HealthData> health() {
        HealthData data = new HealthData(
                true,
                mqttService.isConnected(),
                appName,
                dataSimulated ? "mock" : "real",
                dataSimulated,
                OffsetDateTime.now().toString());
        return ApiResponse.ok(data);
    }

    public record HealthData(
            boolean available,
            boolean mqttConnected,
            String application,
            String mode,
            boolean dataSimulated,
            String time) {
    }
}