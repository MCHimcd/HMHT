package ww.wangnong.bridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import ww.wangnong.config.RunModeHolder;
import ww.wangnong.config.condition.ConditionalOnBridgeMode;
import ww.wangnong.mqtt.MqttSubscriptionRegistry;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.service.AlertService;
import ww.wangnong.service.AlertService.AlertInfo;

/**
 * MQTT topic bridge between Python (agri) and App (wangnong).
 *
 * Subscribes to Python-side topics (/agri/...) and republishes translated
 * messages on App-side topics (/wangnong_2026/...), and vice versa for
 * commands.
 *
 * 在桥接模式下激活（bridge/hardware/full）。
 */
@Component
@ConditionalOnBridgeMode
public class MqttBridge {

    private static final Logger log = LoggerFactory.getLogger(MqttBridge.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    // Python-side topic patterns
    private static final String PY_TELEMETRY = "/agri/device/+/telemetry";
    private static final String PY_AI_ALERT = "/agri/ai_vision/alert";
    private static final String PY_ACK = "/agri/device/+/ack";

    // Hardware-side topic patterns
    private static final String HW_TELEMETRY = "/hmht/device/+/telemetry";
    private static final String HW_ACK = "/hmht/device/+/ack";
    private static final String HW_CAPABILITY = "/hmht/device/+/capability";

    // App-side topic patterns
    private static final String APP_COMMAND = "/wangnong_2026/node/+/command";

    // Topic prefixes
    private static final String APP_TOPIC_PREFIX = "/wangnong_2026/node/";
    private static final String PY_TOPIC_PREFIX = "/agri/device/";
    private static final String HW_TOPIC_PREFIX = "/hmht/device/";
    private static final int BRIDGE_QOS = 1;

    private final PahoMqttService mqtt;
    private final AlertService alertService;
    private final InfluxTelemetryWriter influxWriter;
    private final MqttSubscriptionRegistry subscriptionRegistry;
    private final ProtocolPersistenceService protocolPersistenceService;
    private final RunModeHolder runModeHolder;

    public MqttBridge(PahoMqttService mqtt,
            AlertService alertService,
            InfluxTelemetryWriter influxWriter,
            MqttSubscriptionRegistry subscriptionRegistry,
            ProtocolPersistenceService protocolPersistenceService,
            RunModeHolder runModeHolder) {
        this.mqtt = mqtt;
        this.alertService = alertService;
        this.influxWriter = influxWriter;
        this.subscriptionRegistry = subscriptionRegistry;
        this.protocolPersistenceService = protocolPersistenceService;
        this.runModeHolder = runModeHolder;
    }

    @PostConstruct
    public void init() {
        final boolean enablePythonRoute = runModeHolder.isPythonRouteEnabled();
        final boolean enableHardwareRoute = runModeHolder.isHardwareRouteEnabled();

        if (enablePythonRoute) {
            // 1) Python telemetry → translate → App telemetry + InfluxDB
            subscriptionRegistry.register(PY_TELEMETRY, BRIDGE_QOS, msg -> {
                try {
                    Map<String, Object> pyPayload = MAPPER.readValue(msg.payload(), MAP_TYPE);
                    Map<String, Object> appPayload = TelemetryTranslator.pyToApp(pyPayload);
                    forwardTelemetry(msg.topic(), appPayload);

                    protocolPersistenceService.recordIngestAudit(
                            "telemetry",
                            msg.topic(),
                            str(pyPayload.get("device_id")),
                            str(pyPayload.get("protocol_version")),
                            str(pyPayload.get("trace_id")),
                            "forwarded",
                            null);
                } catch (Exception e) {
                    log.warn("[Bridge] Failed to bridge telemetry: {}", msg.topic(), e);
                    protocolPersistenceService.recordDeadletter(
                            msg.topic(),
                            "",
                            "telemetry",
                            e.getMessage(),
                            msg.payload(),
                            "");
                }
            });

            // 2) Python AI vision alert → translate to AlertInfo → store + publish to App
            subscriptionRegistry.register(PY_AI_ALERT, BRIDGE_QOS, msg -> {
                try {
                    Map<String, Object> pyAlert = MAPPER.readValue(msg.payload(), MAP_TYPE);
                    bridgeAiAlert(pyAlert);
                } catch (Exception e) {
                    log.warn("[Bridge] Failed to bridge AI alert", e);
                }
            });

            // 4) Python command ACK → log only
            subscriptionRegistry.register(PY_ACK, BRIDGE_QOS, msg -> {
                log.info("[Bridge] Command ACK received: topic={}, payload={}", msg.topic(), msg.payload());
                protocolPersistenceService.recordIngestAudit(
                        "ack",
                        msg.topic(),
                        extractDeviceId(msg.topic(), PY_TOPIC_PREFIX),
                        "1.0",
                        "",
                        "received",
                        null);
            });
        }

        if (enableHardwareRoute) {
            // 1.1) Hardware V2 telemetry → normalize → App telemetry + InfluxDB
            subscriptionRegistry.register(HW_TELEMETRY, BRIDGE_QOS, msg -> {
                log.info("[Bridge][HW-IN] topic={}, qos={}, retained={}, payload={}",
                        msg.topic(),
                        msg.qos(),
                        msg.retained(),
                        msg.payload());
                try {
                    Map<String, Object> hwPayload = MAPPER.readValue(msg.payload(), MAP_TYPE);
                    String topicDeviceId = extractDeviceId(msg.topic(), HW_TOPIC_PREFIX);
                    if ((hwPayload.get("device_id") == null || String.valueOf(hwPayload.get("device_id")).isBlank())
                            && !topicDeviceId.isBlank()) {
                        hwPayload.put("device_id", topicDeviceId);
                    }

                    protocolPersistenceService.recordTelemetryHeartbeat(
                            str(hwPayload.get("device_id")),
                            toLong(hwPayload.get("timestamp")));

                    Map<String, Object> appPayload = TelemetryTranslator.hardwareV2ToApp(hwPayload);
                    forwardTelemetry(msg.topic(), appPayload);

                    protocolPersistenceService.recordIngestAudit(
                            "telemetry",
                            msg.topic(),
                            str(hwPayload.get("device_id")),
                            str(hwPayload.get("protocol_version")),
                            str(hwPayload.get("trace_id")),
                            "forwarded",
                            null);
                } catch (Exception e) {
                    log.warn("[Bridge] Failed to bridge hardware telemetry: {}", msg.topic(), e);
                    protocolPersistenceService.recordDeadletter(
                            msg.topic(),
                            "",
                            "telemetry",
                            e.getMessage(),
                            msg.payload(),
                            "");
                }
            });

            // 1.2) Hardware capability declaration → control-plane persistence
            subscriptionRegistry.register(HW_CAPABILITY, BRIDGE_QOS, msg -> {
                log.info("[Bridge][HW-IN] topic={}, qos={}, retained={}, payload={}",
                        msg.topic(),
                        msg.qos(),
                        msg.retained(),
                        msg.payload());
                try {
                    Map<String, Object> capabilityPayload = MAPPER.readValue(msg.payload(), MAP_TYPE);
                    String topicDeviceId = extractDeviceId(msg.topic(), HW_TOPIC_PREFIX);
                    if ((capabilityPayload.get("device_id") == null
                            || String.valueOf(capabilityPayload.get("device_id")).isBlank())
                            && !topicDeviceId.isBlank()) {
                        capabilityPayload.put("device_id", topicDeviceId);
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = extractPayloadMap(capabilityPayload);
                    Map<String, Object> sensors = payload.get("sensors") instanceof Map<?, ?> map
                            ? (Map<String, Object>) map
                            : Map.of();
                    Map<String, Object> equipments = payload.get("equipments") instanceof Map<?, ?> map
                            ? (Map<String, Object>) map
                            : Map.of();

                    String deviceId = firstNonBlank(str(capabilityPayload.get("device_id")), topicDeviceId);
                    String capabilityVersion = firstNonBlank(
                            str(payload.get("capability_version")),
                            str(capabilityPayload.get("protocol_version")),
                            "2.0");
                    String checksum = firstNonBlank(str(payload.get("checksum")),
                            str(capabilityPayload.get("checksum")));
                    Long timestamp = toLong(capabilityPayload.get("timestamp"));
                    String traceId = firstNonBlank(
                            str(capabilityPayload.get("trace_id")),
                            str(payload.get("trace_id")));

                    protocolPersistenceService.recordCapabilitySnapshot(
                            deviceId,
                            capabilityVersion,
                            checksum,
                            sensors,
                            equipments,
                            timestamp,
                            msg.topic(),
                            msg.payload(),
                            traceId);
                } catch (Exception e) {
                    log.warn("[Bridge] Failed to persist capability snapshot: {}", msg.topic(), e);
                    protocolPersistenceService.recordDeadletter(
                            msg.topic(),
                            extractDeviceId(msg.topic(), HW_TOPIC_PREFIX),
                            "capability",
                            e.getMessage(),
                            msg.payload(),
                            "");
                }
            });

            // 4.1) Hardware ACK → relay to App ACK topic + log
            subscriptionRegistry.register(HW_ACK, BRIDGE_QOS, msg -> {
                log.info("[Bridge][HW-IN] topic={}, qos={}, retained={}, payload={}",
                        msg.topic(),
                        msg.qos(),
                        msg.retained(),
                        msg.payload());
                try {
                    Map<String, Object> ackPayload = MAPPER.readValue(msg.payload(), MAP_TYPE);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ackData = extractPayloadMap(ackPayload);
                    String nodeId = firstNonBlank(
                            str(ackPayload.get("device_id")),
                            extractDeviceId(msg.topic(), HW_TOPIC_PREFIX));

                    if (nodeId.isBlank()) {
                        log.warn("[Bridge] Hardware ACK missing device_id: topic={}", msg.topic());
                        return;
                    }

                    String appAckTopic = APP_TOPIC_PREFIX + nodeId + "/ack";
                    mqtt.publish(appAckTopic, msg.payload(), 1, false);

                    String commandId = str(ackData.get("command_id"));
                    String status = str(ackData.get("status"));
                    Integer ackCode = toInt(ackData.get("ack_code"));
                    String ackMessage = str(ackData.get("ack_message"));
                    Long ackedAt = toLong(ackData.get("acked_at"));
                    String traceId = firstNonBlank(str(ackPayload.get("trace_id")), str(ackData.get("trace_id")));

                    protocolPersistenceService.recordAckReceived(
                            nodeId,
                            commandId,
                            status,
                            ackCode,
                            ackMessage,
                            ackedAt,
                            msg.payload(),
                            msg.topic(),
                            traceId);

                    protocolPersistenceService.recordIngestAudit(
                            "ack",
                            msg.topic(),
                            nodeId,
                            str(ackPayload.get("protocol_version")),
                            traceId,
                            "relayed",
                            null);

                    log.info("[Bridge] Hardware ACK relayed: {} → {}", msg.topic(), appAckTopic);
                } catch (Exception e) {
                    log.warn("[Bridge] Failed to relay hardware ACK: {}", msg.topic(), e);
                    protocolPersistenceService.recordDeadletter(
                            msg.topic(),
                            extractDeviceId(msg.topic(), HW_TOPIC_PREFIX),
                            "ack",
                            e.getMessage(),
                            msg.payload(),
                            "");
                }
            });
        }

        // 3) App command → forward based on run mode route
        subscriptionRegistry.register(APP_COMMAND, BRIDGE_QOS, msg -> {
            try {
                // Extract nodeId from topic: /wangnong_2026/node/{nodeId}/command
                String topic = msg.topic();
                String[] parts = topic.split("/");
                // parts: ["", "wangnong_2026", "node", "{nodeId}", "command"]
                if (parts.length < 5)
                    return;
                String deviceId = parts[3];

                String pyTopic = PY_TOPIC_PREFIX + deviceId + "/command";
                String hwTopic = HW_TOPIC_PREFIX + deviceId + "/command";

                HardwareCommandEnvelope hwEnvelope = null;
                String protocolVersion = "1.0";
                String traceId = "";

                if (enablePythonRoute) {
                    mqtt.publish(pyTopic, msg.payload(), 1, false);
                }
                if (enableHardwareRoute) {
                    hwEnvelope = toHardwareCommandEnvelope(deviceId, msg.payload());
                    mqtt.publish(hwTopic, hwEnvelope.rawJson(), 1, false);

                    protocolPersistenceService.recordCommandDispatched(
                            deviceId,
                            hwEnvelope.commandId(),
                            hwEnvelope.targetKey(),
                            hwEnvelope.action(),
                            hwEnvelope.traceId(),
                            hwEnvelope.timeoutMs(),
                            topic,
                            hwTopic,
                            hwEnvelope.rawJson());

                    protocolVersion = hwEnvelope.protocolVersion();
                    traceId = hwEnvelope.traceId();
                }

                protocolPersistenceService.recordIngestAudit(
                        "command",
                        topic,
                        deviceId,
                        protocolVersion,
                        traceId,
                        "forwarded",
                        null);

                if (enablePythonRoute && enableHardwareRoute) {
                    log.debug("[Bridge] Command forwarded: {} → {} and {}", topic, pyTopic, hwTopic);
                } else if (enablePythonRoute) {
                    log.debug("[Bridge] Command forwarded: {} → {}", topic, pyTopic);
                } else {
                    log.debug("[Bridge] Command forwarded: {} → {}", topic, hwTopic);
                }
            } catch (Exception e) {
                log.warn("[Bridge] Failed to bridge command: {}", msg.topic(), e);
                protocolPersistenceService.recordDeadletter(
                        msg.topic(),
                        extractDeviceId(msg.topic(), APP_TOPIC_PREFIX),
                        "command",
                        e.getMessage(),
                        msg.payload(),
                        "");
            }
        });

        subscriptionRegistry.bindAll(mqtt);

        log.info("[Bridge] MQTT Bridge initialized: mode={}, pythonRoute={}, hardwareRoute={}",
                runModeHolder.getMode(),
                enablePythonRoute,
                enableHardwareRoute);
    }

    private void forwardTelemetry(String sourceTopic, Map<String, Object> appPayload) throws Exception {
        String nodeId = str(appPayload.get("node_id"));
        if (nodeId.isBlank()) {
            log.warn("[Bridge] Ignore telemetry without node_id: sourceTopic={}", sourceTopic);
            return;
        }

        String appTopic = APP_TOPIC_PREFIX + nodeId + "/telemetry";
        mqtt.publish(appTopic, MAPPER.writeValueAsString(appPayload), 1, false);
        influxWriter.writeTelemetry(nodeId, appPayload);
        log.debug("[Bridge] Telemetry forwarded: {} → {}", sourceTopic, appTopic);
    }

    @SuppressWarnings("unchecked")
    private HardwareCommandEnvelope toHardwareCommandEnvelope(String deviceId, String appPayloadRaw) {
        try {
            Map<String, Object> command = MAPPER.readValue(appPayloadRaw, MAP_TYPE);

            if ("command".equals(str(command.get("msg_type"))) && command.get("payload") instanceof Map<?, ?>) {
                command.putIfAbsent("protocol_version", "2.0");
                command.putIfAbsent("device_id", deviceId);
                command.putIfAbsent("timestamp", System.currentTimeMillis());
                command.putIfAbsent("trace_id", UUID.randomUUID().toString());
                Map<String, Object> payload = (Map<String, Object>) command.get("payload");
                String commandId = firstNonBlank(str(payload.get("command_id")),
                        "CMD-" + UUID.randomUUID().toString().substring(0, 8));
                payload.put("command_id", commandId);
                String targetKey = firstNonBlank(str(payload.get("target_key")), "unknown");
                String action = firstNonBlank(str(payload.get("action")), "noop");
                int timeoutMs = toInt(payload.get("timeout_ms"), 5000);
                String traceId = str(command.get("trace_id"));
                String protocolVersion = firstNonBlank(str(command.get("protocol_version")), "2.0");
                String rawJson = MAPPER.writeValueAsString(command);
                return new HardwareCommandEnvelope(rawJson, commandId, targetKey, action, traceId, timeoutMs,
                        protocolVersion);
            }

            Map<String, Object> action = command.get("action") instanceof Map<?, ?>
                    ? (Map<String, Object>) command.get("action")
                    : Map.of();

            String targetKey = firstNonBlank(
                    str(action.get("target_key")),
                    str(action.get("target_device")),
                    str(command.get("target_key")),
                    str(command.get("target_device")));

            String actionName = firstNonBlank(
                    str(action.get("action")),
                    str(action.get("command")),
                    str(command.get("action")),
                    str(command.get("command")));

            String commandId = "CMD-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> commandPayload = new LinkedHashMap<>();
            commandPayload.put("command_id", commandId);
            commandPayload.put("target_key", targetKey.isBlank() ? "unknown" : targetKey);
            commandPayload.put("action", actionName.isBlank() ? "noop" : actionName);

            Object paramsObj = action.get("params");
            if (!(paramsObj instanceof Map<?, ?>) && command.get("params") instanceof Map<?, ?>) {
                paramsObj = command.get("params");
            }
            if (paramsObj instanceof Map<?, ?> params) {
                commandPayload.put("params", new LinkedHashMap<>((Map<String, Object>) params));
            }

            int timeoutMs = toInt(command.getOrDefault("timeout_ms", 5000), 5000);
            commandPayload.put("timeout_ms", timeoutMs);

            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("msg_type", "command");
            envelope.put("protocol_version", "2.0");
            envelope.put("device_id", deviceId);
            envelope.put("timestamp", System.currentTimeMillis());
            String traceId = UUID.randomUUID().toString();
            envelope.put("trace_id", traceId);
            envelope.put("payload", commandPayload);
            String rawJson = MAPPER.writeValueAsString(envelope);
            return new HardwareCommandEnvelope(rawJson,
                    commandId,
                    commandPayload.get("target_key").toString(),
                    commandPayload.get("action").toString(),
                    traceId,
                    timeoutMs,
                    "2.0");
        } catch (Exception parseError) {
            String commandId = "CMD-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("command_id", commandId);
            payload.put("target_key", "unknown");
            payload.put("action", "noop");
            payload.put("raw_payload", appPayloadRaw);
            payload.put("timeout_ms", 5000);

            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("msg_type", "command");
            envelope.put("protocol_version", "2.0");
            envelope.put("device_id", deviceId);
            envelope.put("timestamp", System.currentTimeMillis());
            String traceId = UUID.randomUUID().toString();
            envelope.put("trace_id", traceId);
            envelope.put("payload", payload);
            try {
                String rawJson = MAPPER.writeValueAsString(envelope);
                return new HardwareCommandEnvelope(rawJson, commandId, "unknown", "noop", traceId, 5000, "2.0");
            } catch (Exception writeError) {
                return new HardwareCommandEnvelope("{}", commandId, "unknown", "noop", traceId, 5000, "2.0");
            }
        }
    }

    private static String extractDeviceId(String topic, String topicPrefix) {
        if (topic == null || topic.isBlank() || !topic.startsWith(topicPrefix)) {
            return "";
        }
        String remain = topic.substring(topicPrefix.length());
        int slashIndex = remain.indexOf('/');
        if (slashIndex <= 0) {
            return "";
        }
        return remain.substring(0, slashIndex);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractPayloadMap(Map<String, Object> envelope) {
        Object payload = envelope.get("payload");
        if (payload instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return envelope;
    }

    private static Integer toInt(Object value) {
        return toInt(value, null);
    }

    private static int toInt(Object value, int fallback) {
        Integer parsed = toInt(value, null);
        return parsed == null ? fallback : parsed;
    }

    private static Integer toInt(Object value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private record HardwareCommandEnvelope(
            String rawJson,
            String commandId,
            String targetKey,
            String action,
            String traceId,
            Integer timeoutMs,
            String protocolVersion) {
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
