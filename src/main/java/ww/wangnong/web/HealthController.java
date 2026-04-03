package ww.wangnong.web;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.mqtt.PahoMqttService;

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