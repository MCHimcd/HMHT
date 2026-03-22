package ww.wangnong.mock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import ww.wangnong.mqtt.PahoMqttService;

@Component
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "mock", matchIfMissing = true)
public class MockMqttSimulator {

    private static final Logger log = LoggerFactory.getLogger(MockMqttSimulator.class);
    private static final Random RNG = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String NODE_ID = "A03_Node_01";
    private static final String TELEMETRY_TOPIC = "/wangnong_2026/node/" + NODE_ID + "/telemetry";
    private static final String COMMAND_TOPIC = "/wangnong_2026/node/+/command";

    private final PahoMqttService mqttService;

    private final ConcurrentHashMap<String, Double> environment = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> equipment = new ConcurrentHashMap<>();
    private volatile String mode = "manual";

    public MockMqttSimulator(PahoMqttService mqttService) {
        this.mqttService = mqttService;
        environment.put("temperature", 25.0);
        environment.put("humidity", 60.0);
        environment.put("light", 10000.0);
        environment.put("co2", 400.0);
        environment.put("soilTemp", 20.0);
        environment.put("soilHumidity", 40.0);

        equipment.put("water_valve", false);
        equipment.put("water_pump", false);
        equipment.put("led_light", false);
        equipment.put("camera", false);
    }

    @PostConstruct
    public void init() {
        mqttService.subscribe(COMMAND_TOPIC, msg -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = MAPPER.readValue(msg.payload(), Map.class);
                handleCommand(payload);
            } catch (Exception e) {
                log.warn("[MockMqtt] Failed to handle command", e);
            }
        });
        log.info("[MockMqtt] Simulator initialized, subscribed to {}", COMMAND_TOPIC);
    }

    @Scheduled(fixedDelay = 3_000, initialDelay = 1_000)
    public void publishTelemetry() {
        if (!mqttService.isConnected()) return;

        fluctuate("temperature", 0.3, 15.0, 45.0);
        fluctuate("humidity", 1.0, 20.0, 99.0);
        fluctuate("light", 200.0, 0.0, 50000.0);
        fluctuate("co2", 5.0, 200.0, 2000.0);
        fluctuate("soilTemp", 0.2, 5.0, 40.0);
        fluctuate("soilHumidity", 0.5, 10.0, 90.0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("environment", new LinkedHashMap<>(environment));
        data.put("equipment", new LinkedHashMap<>(equipment));
        data.put("mode", mode);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msg_type", "telemetry");
        payload.put("node_id", NODE_ID);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("data", data);

        try {
            mqttService.publish(TELEMETRY_TOPIC, MAPPER.writeValueAsString(payload), 1, false);
        } catch (Exception e) {
            log.warn("[MockMqtt] Failed to publish telemetry", e);
        }
    }

    private void fluctuate(String key, double step, double min, double max) {
        double current = environment.getOrDefault(key, 25.0);
        double delta = (RNG.nextDouble() * 2 - 1) * step;
        double next = Math.max(min, Math.min(max, current + delta));
        environment.put(key, Math.round(next * 10.0) / 10.0);
    }

    @SuppressWarnings("unchecked")
    private void handleCommand(Map<String, Object> payload) {
        Object actionObj = payload.get("action");
        if (!(actionObj instanceof Map<?, ?> actionMap)) return;
        Map<String, Object> action = (Map<String, Object>) actionMap;

        String targetDevice = String.valueOf(action.get("target_device"));
        String command = String.valueOf(action.get("command"));

        if ("mode".equals(targetDevice)) {
            mode = command;
            log.info("[MockMqtt] Mode changed to: {}", mode);
        } else if (equipment.containsKey(targetDevice)) {
            equipment.put(targetDevice, "turn_on".equals(command));
            log.info("[MockMqtt] {} = {}", targetDevice, equipment.get(targetDevice));
        }
    }
}
