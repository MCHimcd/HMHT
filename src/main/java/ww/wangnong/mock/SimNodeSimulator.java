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

import jakarta.annotation.PostConstruct;
import ww.wangnong.bridge.InfluxTelemetryWriter;
import ww.wangnong.bridge.TelemetryAlertChecker;
import ww.wangnong.config.RunModeHolder;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.NodeRepository;

/**
 * Purpose: 为 SIM_Node_01 生成完整的单节点模拟遥测与告警。
 * Responsibilities: 维护模拟环境/设备状态、响应 command、发布 telemetry、写入 Influx，并按阈值制造异常演示。
 * Flow: 启动时订阅节点 command -> 周期性波动环境并发布 telemetry -> 定时注入异常并触发告警检查。
 * Dependencies:
 * PahoMqttService、TelemetryAlertChecker、InfluxTelemetryWriter、NodeRepository、RunModeHolder。
 * Input: 节点 command、数据库中的节点存在性、随机数。
 * Output: SIM_Node_01 的 telemetry、告警与 Influx 时序数据。
 * Boundary: hardware 模式下强制禁用；节点若已从数据库删除会自动停机，避免幽灵节点继续发数据。
 */
@Component
public class SimNodeSimulator {

    private static final Logger log = LoggerFactory.getLogger(SimNodeSimulator.class);
    private static final Random RNG = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String NODE_ID = "SIM_Node_01";
    private static final String COMMAND_TOPIC = "/wangnong_2026/node/" + NODE_ID + "/command";

    private final PahoMqttService mqttService;
    private final TelemetryAlertChecker alertChecker;
    private final InfluxTelemetryWriter influxWriter;
    private final NodeRepository nodeRepo;

    private final AtomicBoolean active = new AtomicBoolean(true);

    private final ConcurrentHashMap<String, Double> env = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> equip = new ConcurrentHashMap<>();
    private int ledBrightness = 0;
    private String mode = "manual";
    private int anomalyCycle = 0;

    private static final String[] SPIKE_KEYS = { "temperature", "humidity", "co2" };
    private static final double[] SPIKE_VALUES = { 33.0, 78.0, 480.0 };

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
        env.put("light", 900.0);
        env.put("co2", 420.0 + RNG.nextDouble() * 10);
        env.put("soilTemp", 19.0 + RNG.nextDouble() * 2);
        env.put("soilHumidity", 68.0 + RNG.nextDouble() * 4);
        env.put("soilPh", 6.6 + RNG.nextDouble() * 0.4);
        env.put("soilN", 150.0 + RNG.nextDouble() * 30);
        env.put("soilP", 150.0 + RNG.nextDouble() * 30);
        env.put("soilK", 150.0 + RNG.nextDouble() * 30);

        equip.put("fan", false);
        equip.put("water_valve", false);
        equip.put("water_pump", false);
        equip.put("led_light", false);
        equip.put("camera", true);
        equip.put("heater", false);
        equip.put("shade", false);
        equip.put("ventilation", false);
        equip.put("fertilizer_pump", false);
        equip.put("mist_sprayer", false);
        equip.put("fertigation_machine", false);
        equip.put("air_source_ac", false);
        equip.put("led_display", false);
        equip.put("electric_skylight", false);
    }

    @PostConstruct
    public void init() {
        if (!active.get()) {
            return;
        }
        mqttService.subscribe(COMMAND_TOPIC, msg -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = MAPPER.readValue(msg.payload(), Map.class);
                handleCommand(payload);
            } catch (Exception e) {
                log.warn("[SimNode] Failed to handle command", e);
            }
        });
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

        // Reason: 低频检查数据库存在性，避免每轮 telemetry 都访问数据库，同时能在删除节点后及时停机。
        if (anomalyCycle % 10 == 0 && nodeRepo.findByNodeId(NODE_ID).isEmpty()) {
            log.info("[SimNode] Node {} no longer in DB, stopping simulator.", NODE_ID);
            shutdown();
            return;
        }

        fluctuate("temperature", 0.3, 20.0, 30.0);
        fluctuate("humidity", 1.0, 50.0, 70.0);
        fluctuate("light", 50.0, 300.0, 1500.0);
        fluctuate("co2", 2.0, 400.0, 450.0);
        fluctuate("soilTemp", 0.2, 15.0, 25.0);
        fluctuate("soilHumidity", 0.5, 60.0, 80.0);
        fluctuate("soilPh", 0.05, 6.0, 7.5);
        fluctuate("soilN", 3.0, 80.0, 250.0);
        fluctuate("soilP", 3.0, 80.0, 250.0);
        fluctuate("soilK", 3.0, 80.0, 250.0);

        if ("smart".equals(mode)) {
            double light = env.getOrDefault("light", 900.0);
            ledBrightness = Math.max(0, Math.min(100, (int) ((1.0 - light / 1500.0) * 100)));
            equip.put("led_light", ledBrightness > 0);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> equipData = new LinkedHashMap<>(equip);
        equipData.put("led_light", ledBrightness);
        data.put("environment", new LinkedHashMap<>(env));
        data.put("equipment", equipData);
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

    @SuppressWarnings("unchecked")
    private void handleCommand(Map<String, Object> payload) {
        Object actionObj = payload.get("action");
        if (!(actionObj instanceof Map<?, ?> actionMap)) {
            return;
        }
        Map<String, Object> action = (Map<String, Object>) actionMap;
        String targetDevice = String.valueOf(action.getOrDefault("target_device", ""));
        String command = String.valueOf(action.getOrDefault("command", ""));

        if ("mode".equals(targetDevice)) {
            mode = command;
            log.info("[SimNode] mode changed to {}", mode);
            return;
        }

        if (!equip.containsKey(targetDevice)) {
            return;
        }

        if ("led_light".equals(targetDevice)) {
            if ("set_brightness".equals(command)) {
                Map<String, Object> params = action.get("parameters") instanceof Map<?, ?>
                        ? (Map<String, Object>) action.get("parameters")
                        : Map.of();
                Object brightnessValue = params.get("brightness");
                if (brightnessValue instanceof Number number) {
                    ledBrightness = Math.max(0, Math.min(100, number.intValue()));
                }
            } else if ("turn_on".equals(command)) {
                ledBrightness = 100;
            } else if ("turn_off".equals(command)) {
                ledBrightness = 0;
            }
            equip.put(targetDevice, ledBrightness > 0);
            log.info("[SimNode] led_light brightness = {}", ledBrightness);
            return;
        }

        boolean on = "turn_on".equals(command);
        equip.put(targetDevice, on);
        log.info("[SimNode] {} = {}", targetDevice, on);
    }
}
