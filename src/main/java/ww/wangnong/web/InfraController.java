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
            mqttService.publish(request.topic(), request.payload(), request.qos(), Boolean.TRUE.equals(request.retained()));
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
