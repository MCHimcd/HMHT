package ww.wangnong.mock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ww.wangnong.bridge.InfluxTelemetryWriter;
import ww.wangnong.bridge.TelemetryAlertChecker;
import ww.wangnong.config.RunModeHolder;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.NodeRepository;

/**
 * SIM_Node_01 模拟器：在所有运行模式下为全模拟演示节点生成遥测数据。
 * 若节点从数据库删除则自动停止；可通过 {@link #shutdown()} 外部终止。
 */
@Component
public class SimNodeSimulator {

    private static final Logger log = LoggerFactory.getLogger(SimNodeSimulator.class);
    private static final Random RNG = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String NODE_ID = "SIM_Node_01";

    private final PahoMqttService mqttService;
    private final TelemetryAlertChecker alertChecker;
    private final InfluxTelemetryWriter influxWriter;
    private final NodeRepository nodeRepo;

    private final AtomicBoolean active = new AtomicBoolean(true);

    private final ConcurrentHashMap<String, Double> env = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> equip = new ConcurrentHashMap<>();
    private String mode = "manual";
    private int anomalyCycle = 0;

    private static final String[] SPIKE_KEYS = { "temperature", "humidity", "co2" };
    private static final double[] SPIKE_VALUES = { 37.5, 88.0, 1650.0 };

    public SimNodeSimulator(PahoMqttService mqttService,
            TelemetryAlertChecker alertChecker,
            InfluxTelemetryWriter influxWriter,
            NodeRepository nodeRepo,
            RunModeHolder runModeHolder) {
        this.mqttService = mqttService;
        this.alertChecker = alertChecker;
        this.influxWriter = influxWriter;
        this.nodeRepo = nodeRepo;

        // hardware 模式下不需要模拟节点
        if (runModeHolder.getMode() == ww.wangnong.config.RunMode.HARDWARE) {
            active.set(false);
            log.info("[SimNode] Disabled in hardware mode.");
            return;
        }

        env.put("temperature", 24.0 + RNG.nextDouble() * 2);
        env.put("humidity", 58.0 + RNG.nextDouble() * 5);
        env.put("light", 12000.0);
        env.put("co2", 430.0 + RNG.nextDouble() * 20);
        env.put("soilTemp", 20.0 + RNG.nextDouble());
        env.put("soilHumidity", 42.0 + RNG.nextDouble() * 3);

        equip.put("water_valve", false);
        equip.put("water_pump", false);
        equip.put("led_light", false);
        equip.put("camera", true);
    }

    /** 外部终止模拟（如收到真实硬件数据后调用）。 */
    public void shutdown() {
        if (active.compareAndSet(true, false)) {
            log.info("[SimNode] Simulator shut down.");
        }
    }

    public boolean isActive() {
        return active.get();
    }

    @Scheduled(fixedDelay = 3_000, initialDelay = 2_000)
    public void publishTelemetry() {
        if (!active.get() || !mqttService.isConnected())
            return;

        // 每 10 轮（~30s）查一次数据库，节点删除则停止
        if (anomalyCycle % 10 == 0 && nodeRepo.findByNodeId(NODE_ID).isEmpty()) {
            log.info("[SimNode] Node {} no longer in DB, stopping simulator.", NODE_ID);
            shutdown();
            return;
        }

        fluctuate("temperature", 0.3, 15.0, 45.0);
        fluctuate("humidity", 1.0, 20.0, 99.0);
        fluctuate("light", 200.0, 0.0, 50000.0);
        fluctuate("co2", 5.0, 200.0, 2000.0);
        fluctuate("soilTemp", 0.2, 5.0, 40.0);
        fluctuate("soilHumidity", 0.5, 10.0, 90.0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("environment", new LinkedHashMap<>(env));
        data.put("equipment", new LinkedHashMap<>(equip));
        data.put("mode", mode);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msg_type", "telemetry");
        payload.put("node_id", NODE_ID);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("data", data);

        try {
            String topic = "/wangnong_2026/node/" + NODE_ID + "/telemetry";
            mqttService.publish(topic, MAPPER.writeValueAsString(payload), 1, false);
            influxWriter.writeTelemetry(NODE_ID, payload);
        } catch (Exception e) {
            log.warn("[SimNode] Failed to publish telemetry", e);
        }
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void checkThresholds() {
        if (!active.get() || !mqttService.isConnected())
            return;

        anomalyCycle++;
        if (anomalyCycle % 3 == 0) {
            int idx = RNG.nextInt(SPIKE_KEYS.length);
            String key = SPIKE_KEYS[idx];
            double spike = SPIKE_VALUES[idx];
            env.put(key, spike);
            log.info("[SimNode] Anomaly injected: {}={}", key, spike);
        }

        alertChecker.checkAndAlertEnv(NODE_ID, env);
    }

    private void fluctuate(String key, double step, double min, double max) {
        double current = env.getOrDefault(key, 25.0);
        double delta = (RNG.nextDouble() * 2 - 1) * step;
        double next = Math.max(min, Math.min(max, current + delta));
        env.put(key, Math.round(next * 10.0) / 10.0);
    }
}
