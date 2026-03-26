package ww.wangnong.mock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.influx.InfluxTelemetryService;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.service.AlertService;

@Component
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "mock", matchIfMissing = true)
public class MockMqttSimulator {

    private static final Logger log = LoggerFactory.getLogger(MockMqttSimulator.class);
    private static final Random RNG = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String[] NODE_IDS = { "A03_Node_01", "A03_Node_02" };
    private static final String COMMAND_TOPIC = "/wangnong_2026/node/+/command";

    private final PahoMqttService mqttService;
    @Nullable
    private final InfluxTelemetryService influxService;
    private final NodeSensorRepository nodeSensorRepo;
    private final AlertService alertService;

    /** Per-node environment data: nodeId -> (sensorKey -> value) */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> nodeEnvironments = new ConcurrentHashMap<>();
    /** Per-node equipment state: nodeId -> (equipKey -> on/off) */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> nodeEquipments = new ConcurrentHashMap<>();
    /** Per-node mode */
    private final ConcurrentHashMap<String, String> nodeModes = new ConcurrentHashMap<>();
    private final AtomicLong alertSeq = new AtomicLong(System.currentTimeMillis());
    /** Cooldown: "nodeId:sensorKey" -> last alert epoch ms */
    private final ConcurrentHashMap<String, Long> lastAlertTime = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_MS = 5 * 60_000L;

    /** sensor_key -> human-readable name for alert messages */
    private static final Map<String, String> SENSOR_NAMES = Map.of(
            "temperature", "空气温度",
            "humidity", "空气湿度",
            "co2", "CO₂浓度",
            "light", "光照强度",
            "soilTemp", "土壤温度",
            "soilHumidity", "土壤湿度");
    private static final Map<String, String> SENSOR_UNITS = Map.of(
            "temperature", "℃",
            "humidity", "%",
            "co2", "ppm",
            "light", "Lux",
            "soilTemp", "℃",
            "soilHumidity", "%");

    public MockMqttSimulator(PahoMqttService mqttService,
            @Nullable InfluxTelemetryService influxService,
            NodeSensorRepository nodeSensorRepo,
            AlertService alertService) {
        this.mqttService = mqttService;
        this.influxService = influxService;
        this.nodeSensorRepo = nodeSensorRepo;
        this.alertService = alertService;

        for (String nodeId : NODE_IDS) {
            ConcurrentHashMap<String, Double> env = new ConcurrentHashMap<>();
            env.put("temperature", 25.0 + RNG.nextDouble() * 2);
            env.put("humidity", 58.0 + RNG.nextDouble() * 4);
            env.put("light", 10000.0);
            env.put("co2", 400.0 + RNG.nextDouble() * 20);
            env.put("soilTemp", 20.0 + RNG.nextDouble());
            env.put("soilHumidity", 40.0 + RNG.nextDouble() * 2);
            nodeEnvironments.put(nodeId, env);

            ConcurrentHashMap<String, Boolean> eq = new ConcurrentHashMap<>();
            eq.put("water_valve", false);
            eq.put("water_pump", false);
            eq.put("led_light", false);
            eq.put("camera", false);
            nodeEquipments.put(nodeId, eq);

            nodeModes.put(nodeId, "manual");
        }
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
        if (!mqttService.isConnected())
            return;

        for (String nodeId : NODE_IDS) {
            ConcurrentHashMap<String, Double> env = nodeEnvironments.get(nodeId);
            ConcurrentHashMap<String, Boolean> eq = nodeEquipments.get(nodeId);
            if (env == null || eq == null)
                continue;

            fluctuate(env, "temperature", 0.3, 15.0, 45.0);
            fluctuate(env, "humidity", 1.0, 20.0, 99.0);
            fluctuate(env, "light", 200.0, 0.0, 50000.0);
            fluctuate(env, "co2", 5.0, 200.0, 2000.0);
            fluctuate(env, "soilTemp", 0.2, 5.0, 40.0);
            fluctuate(env, "soilHumidity", 0.5, 10.0, 90.0);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("environment", new LinkedHashMap<>(env));
            data.put("equipment", new LinkedHashMap<>(eq));
            data.put("mode", nodeModes.getOrDefault(nodeId, "manual"));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("msg_type", "telemetry");
            payload.put("node_id", nodeId);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("data", data);

            try {
                String topic = "/wangnong_2026/node/" + nodeId + "/telemetry";
                mqttService.publish(topic, MAPPER.writeValueAsString(payload), 1, false);
            } catch (Exception e) {
                log.warn("[MockMqtt] Failed to publish telemetry for {}", nodeId, e);
            }

            if (influxService != null) {
                long ts = System.currentTimeMillis();
                influxService.writeEnvironment(nodeId, ts, new LinkedHashMap<>(env));
                influxService.writeEquipmentState(nodeId, ts, new LinkedHashMap<>(eq),
                        nodeModes.getOrDefault(nodeId, "manual"));
            }
        }
    }

    private void fluctuate(ConcurrentHashMap<String, Double> env, String key,
            double step, double min, double max) {
        double current = env.getOrDefault(key, 25.0);
        double delta = (RNG.nextDouble() * 2 - 1) * step;
        double next = Math.max(min, Math.min(max, current + delta));
        env.put(key, Math.round(next * 10.0) / 10.0);
    }

    @SuppressWarnings("unchecked")
    private void handleCommand(Map<String, Object> payload) {
        String nodeId = String.valueOf(payload.getOrDefault("node_id", NODE_IDS[0]));
        Object actionObj = payload.get("action");
        if (!(actionObj instanceof Map<?, ?> actionMap))
            return;
        Map<String, Object> action = (Map<String, Object>) actionMap;

        String targetDevice = String.valueOf(action.get("target_device"));
        String command = String.valueOf(action.get("command"));

        if ("mode".equals(targetDevice)) {
            nodeModes.put(nodeId, command);
            log.info("[MockMqtt] {} mode changed to: {}", nodeId, command);
        } else {
            ConcurrentHashMap<String, Boolean> eq = nodeEquipments.get(nodeId);
            if (eq != null && eq.containsKey(targetDevice)) {
                eq.put(targetDevice, "turn_on".equals(command));
                log.info("[MockMqtt] {} {} = {}", nodeId, targetDevice, eq.get(targetDevice));
            }
        }
    }

    /**
     * Check sensor values against thresholds every 30 seconds for all nodes.
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void checkThresholdsAndAlert() {
        if (!mqttService.isConnected())
            return;

        for (String nodeId : NODE_IDS) {
            ConcurrentHashMap<String, Double> env = nodeEnvironments.get(nodeId);
            if (env == null)
                continue;

            List<NodeSensor> sensors = nodeSensorRepo.findByNodeId(nodeId);
            for (NodeSensor ns : sensors) {
                Double value = env.get(ns.getSensorKey());
                if (value == null)
                    continue;

                String violation = null;
                String level = "warning";
                if (ns.getWarningMax() != null && value > ns.getWarningMax()) {
                    violation = String.format("%s %.1f%s 超过阈值 %.1f%s",
                            SENSOR_NAMES.getOrDefault(ns.getSensorKey(), ns.getSensorKey()),
                            value, SENSOR_UNITS.getOrDefault(ns.getSensorKey(), ""),
                            ns.getWarningMax(), SENSOR_UNITS.getOrDefault(ns.getSensorKey(), ""));
                } else if (ns.getWarningMin() != null && value < ns.getWarningMin()) {
                    violation = String.format("%s %.1f%s 低于阈值 %.1f%s",
                            SENSOR_NAMES.getOrDefault(ns.getSensorKey(), ns.getSensorKey()),
                            value, SENSOR_UNITS.getOrDefault(ns.getSensorKey(), ""),
                            ns.getWarningMin(), SENSOR_UNITS.getOrDefault(ns.getSensorKey(), ""));
                }

                if (violation == null)
                    continue;

                // Rate limit: one alert per node+sensor every 5 minutes
                String cooldownKey = nodeId + ":" + ns.getSensorKey();
                long now = System.currentTimeMillis();
                Long last = lastAlertTime.get(cooldownKey);
                if (last != null && now - last < ALERT_COOLDOWN_MS)
                    continue;
                lastAlertTime.put(cooldownKey, now);

                String alertId = "A" + alertSeq.incrementAndGet();
                String typeName = SENSOR_NAMES.getOrDefault(ns.getSensorKey(), ns.getSensorKey()) + "异常";

                alertService.addAlert(new AlertService.AlertInfo(
                        alertId, nodeId, ns.getSensorKey() + "_alert", typeName,
                        level, violation, ns.getSensorKey(),
                        SENSOR_NAMES.getOrDefault(ns.getSensorKey(), ns.getSensorKey()),
                        false, null, now));

                Map<String, Object> alertData = new LinkedHashMap<>();
                alertData.put("alert_id", alertId);
                alertData.put("type", ns.getSensorKey() + "_alert");
                alertData.put("type_name", typeName);
                alertData.put("level", level);
                alertData.put("timestamp", now);
                alertData.put("message", violation);
                alertData.put("source", ns.getSensorKey());
                alertData.put("source_name", SENSOR_NAMES.getOrDefault(ns.getSensorKey(), ns.getSensorKey()));
                alertData.put("is_handled", false);
                alertData.put("handle_time", null);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("msg_type", "alert");
                payload.put("node_id", nodeId);
                payload.put("timestamp", now);
                payload.put("data", alertData);

                try {
                    String topic = "/wangnong_2026/node/" + nodeId + "/alert";
                    mqttService.publish(topic, MAPPER.writeValueAsString(payload), 1, false);
                    log.info("[MockMqtt] Alert published for {}: {}", nodeId, violation);
                } catch (Exception e) {
                    log.warn("[MockMqtt] Failed to publish alert", e);
                }
            }
        }
    }
}
