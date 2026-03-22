package ww.wangnong.bridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.service.AlertService;
import ww.wangnong.service.AlertService.AlertInfo;

/**
 * MQTT topic bridge between Python (agri) and App (wangnong).
 *
 * Subscribes to Python-side topics (/agri/...) and republishes translated
 * messages on App-side topics (/wangnong_2026/...), and vice versa for commands.
 *
 * Activated by hmht.mqtt.bridge.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "bridge")
public class MqttBridge {

    private static final Logger log = LoggerFactory.getLogger(MqttBridge.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    // Python-side topic patterns
    private static final String PY_TELEMETRY = "/agri/device/+/telemetry";
    private static final String PY_AI_ALERT = "/agri/ai_vision/alert";
    private static final String PY_ACK = "/agri/device/+/ack";

    // App-side topic patterns
    private static final String APP_COMMAND = "/wangnong_2026/node/+/command";

    // Topic prefixes
    private static final String APP_TOPIC_PREFIX = "/wangnong_2026/node/";
    private static final String PY_TOPIC_PREFIX = "/agri/device/";

    private final PahoMqttService mqtt;
    private final AlertService alertService;
    private final InfluxTelemetryWriter influxWriter;

    public MqttBridge(PahoMqttService mqtt, AlertService alertService, InfluxTelemetryWriter influxWriter) {
        this.mqtt = mqtt;
        this.alertService = alertService;
        this.influxWriter = influxWriter;
    }

    @PostConstruct
    public void init() {
        // 1) Python telemetry → translate → App telemetry + InfluxDB
        mqtt.subscribe(PY_TELEMETRY, msg -> {
            try {
                Map<String, Object> pyPayload = MAPPER.readValue(msg.payload(), MAP_TYPE);
                Map<String, Object> appPayload = TelemetryTranslator.pyToApp(pyPayload);
                String nodeId = String.valueOf(appPayload.get("node_id"));

                // Publish to App topic
                String appTopic = APP_TOPIC_PREFIX + nodeId + "/telemetry";
                mqtt.publish(appTopic, MAPPER.writeValueAsString(appPayload), 1, false);

                // Write to InfluxDB
                influxWriter.writeTelemetry(nodeId, appPayload);

                log.debug("[Bridge] Telemetry forwarded: {} → {}", msg.topic(), appTopic);
            } catch (Exception e) {
                log.warn("[Bridge] Failed to bridge telemetry: {}", msg.topic(), e);
            }
        });

        // 2) Python AI vision alert → translate to AlertInfo → store + publish to App
        mqtt.subscribe(PY_AI_ALERT, msg -> {
            try {
                Map<String, Object> pyAlert = MAPPER.readValue(msg.payload(), MAP_TYPE);
                bridgeAiAlert(pyAlert);
            } catch (Exception e) {
                log.warn("[Bridge] Failed to bridge AI alert", e);
            }
        });

        // 3) App command → forward to Python topic
        mqtt.subscribe(APP_COMMAND, msg -> {
            try {
                // Extract nodeId from topic: /wangnong_2026/node/{nodeId}/command
                String topic = msg.topic();
                String[] parts = topic.split("/");
                // parts: ["", "wangnong_2026", "node", "{nodeId}", "command"]
                if (parts.length < 5) return;
                String deviceId = parts[3];

                String pyTopic = PY_TOPIC_PREFIX + deviceId + "/command";
                mqtt.publish(pyTopic, msg.payload(), 1, false);
                log.debug("[Bridge] Command forwarded: {} → {}", topic, pyTopic);
            } catch (Exception e) {
                log.warn("[Bridge] Failed to bridge command: {}", msg.topic(), e);
            }
        });

        // 4) Python command ACK → log only
        mqtt.subscribe(PY_ACK, msg -> {
            log.info("[Bridge] Command ACK received: topic={}, payload={}", msg.topic(), msg.payload());
        });

        log.info("[Bridge] MQTT Bridge initialized — bridging /agri/ ↔ /wangnong_2026/");
    }

    @SuppressWarnings("unchecked")
    private void bridgeAiAlert(Map<String, Object> pyAlert) {
        String nodeId = String.valueOf(pyAlert.getOrDefault("device_id", "AI_Camera_01"));
        long timestamp = pyAlert.get("timestamp") instanceof Number n ? n.longValue() : System.currentTimeMillis();

        Object vrObj = pyAlert.get("vision_result");
        if (!(vrObj instanceof Map<?, ?> vr)) {
            log.warn("[Bridge] AI alert missing vision_result");
            return;
        }
        Map<String, Object> visionResult = (Map<String, Object>) vr;

        String diseaseName = String.valueOf(visionResult.getOrDefault("disease_name", "unknown"));
        String healthStatus = String.valueOf(visionResult.getOrDefault("health_status", ""));
        double confidence = visionResult.get("confidence") instanceof Number n2 ? n2.doubleValue() : 0.0;
        String recommendation = String.valueOf(pyAlert.getOrDefault("recommendation", ""));

        // Map to alert fields
        String alertId = "AI_" + UUID.randomUUID().toString().substring(0, 8);
        String type = diseaseName.toLowerCase().replace(" ", "_");
        String typeName = mapDiseaseToName(diseaseName);

        String level;
        if ("Healthy".equalsIgnoreCase(healthStatus)) {
            return; // Don't create alert for healthy plants
        } else if (confidence >= 0.85) {
            level = "critical";
        } else if (confidence >= 0.6) {
            level = "warning";
        } else {
            level = "info";
        }

        String message = recommendation.isEmpty()
                ? String.format("AI视觉检测到 %s (置信度 %.0f%%)", typeName, confidence * 100)
                : recommendation;

        AlertInfo alertInfo = new AlertInfo(
                alertId, nodeId, type, typeName, level,
                message, "ai_vision", "AI视觉分析",
                false, null, timestamp);

        alertService.addAlert(alertInfo);

        // Publish to App alert topic
        try {
            Map<String, Object> appAlertData = new LinkedHashMap<>();
            appAlertData.put("alert_id", alertId);
            appAlertData.put("type", type);
            appAlertData.put("type_name", typeName);
            appAlertData.put("level", level);
            appAlertData.put("timestamp", timestamp);
            appAlertData.put("message", message);
            appAlertData.put("source", "ai_vision");
            appAlertData.put("source_name", "AI视觉分析");
            appAlertData.put("is_handled", false);
            appAlertData.put("handle_time", null);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("msg_type", "alert");
            payload.put("node_id", nodeId);
            payload.put("timestamp", timestamp);
            payload.put("data", appAlertData);

            // Publish to the first real node topic for App to receive
            String alertTopic = APP_TOPIC_PREFIX + "A03_Node_01/alert";
            mqtt.publish(alertTopic, MAPPER.writeValueAsString(payload), 1, false);
            log.info("[Bridge] AI alert bridged: {} → {}", type, alertTopic);
        } catch (Exception e) {
            log.warn("[Bridge] Failed to publish bridged AI alert", e);
        }
    }

    private static String mapDiseaseToName(String diseaseName) {
        return switch (diseaseName) {
            case "Tomato_Early_blight" -> "番茄早疫病";
            case "Tomato_Late_blight" -> "番茄晚疫病";
            case "Tomato_Leaf_Mold" -> "番茄叶霉病";
            case "Tomato_Septoria_leaf_spot" -> "番茄叶斑病";
            case "Tomato_Bacterial_spot" -> "番茄细菌性斑点";
            case "Tomato_Spider_mites" -> "番茄红蜘蛛";
            case "Tomato_Target_Spot" -> "番茄靶斑病";
            case "Tomato__Tomato_YellowLeaf__Curl_Virus" -> "番茄黄化曲叶病毒";
            case "Tomato__Tomato_mosaic_virus" -> "番茄花叶病毒";
            case "Potato_Early_blight" -> "土豆早疫病";
            case "Potato_Late_blight" -> "土豆晚疫病";
            case "Pepper__bell___Bacterial_spot" -> "辣椒细菌性斑点";
            default -> diseaseName.replace("_", " ");
        };
    }
}
