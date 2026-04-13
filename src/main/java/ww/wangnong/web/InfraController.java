package ww.wangnong.web;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ww.wangnong.mqtt.PahoMqttService;

/**
 * Purpose: 提供基础设施级探活与 MQTT 手工调试入口。
 * Responsibilities: 暴露不走统一 ApiResponse 的 infra 健康信息，并把人工发布请求转换为 PahoMqttService
 * 调用。
 * Flow: 健康检查直接读取配置与 MQTT 状态；发布接口先校验 publish 参数，再映射为 HTTP 状态码。
 * Dependencies: PahoMqttService、Spring Web 异常映射。
 * Input: 基础健康请求或 MQTT 发布请求体。
 * Output: 原始 Map 结构健康信息或发布确认结果。
 * Boundary: infra 端点用于运维和调试，不复用业务层的统一响应包装。
 */
@RestController
@RequestMapping("/infra")
public class InfraController {

    private final PahoMqttService mqttService;

    @Value("${spring.application.name:HMHT}")
    private String appName;

    @Value("${hmht.data.simulated:true}")
    private boolean dataSimulated;

    public InfraController(PahoMqttService mqttService) {
        this.mqttService = mqttService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "application", appName,
                "mode", dataSimulated ? "mock" : "real",
                "dataSimulated", dataSimulated,
                "time", OffsetDateTime.now().toString(),
                "mqttConnected", mqttService.isConnected());
    }

    @PostMapping("/mqtt/publish")
    public ResponseEntity<Map<String, Object>> publish(@RequestBody PublishRequest request) {
        try {
            mqttService.publish(request.topic(), request.payload(), request.qos(),
                    Boolean.TRUE.equals(request.retained()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "topic", request.topic() == null || request.topic().isBlank() ? "default" : request.topic(),
                "publishedAt", OffsetDateTime.now().toString()));
    }

    public record PublishRequest(String topic, String payload, Integer qos, Boolean retained) {
    }
}
