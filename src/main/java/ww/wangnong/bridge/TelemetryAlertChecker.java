package ww.wangnong.bridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.SensorType;
import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.service.AlertService;

/**
 * 通用遥测阈值检查器，供桥接模式和模拟节点共用。
 * 当传感器值超出 node_sensor 的 warning_min/warning_max 时生成告警。
 */
@Component
public class TelemetryAlertChecker {

    private static final Logger log = LoggerFactory.getLogger(TelemetryAlertChecker.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long ALERT_COOLDOWN_MS = 5 * 60_000L;

    private final NodeSensorRepository nodeSensorRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final AlertService alertService;
    private final PahoMqttService mqttService;
    private final AtomicLong alertSeq = new AtomicLong(System.currentTimeMillis());
    private final ConcurrentHashMap<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    public TelemetryAlertChecker(NodeSensorRepository nodeSensorRepo,
            SensorTypeRepository sensorTypeRepo,
            AlertService alertService,
            PahoMqttService mqttService) {
        this.nodeSensorRepo = nodeSensorRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.alertService = alertService;
        this.mqttService = mqttService;
    }

    /**
     * 检查给定节点的环境数据是否超出阈值，超出则生成告警并发布 MQTT 告警消息。
     */
    @SuppressWarnings("unchecked")
    public void checkAndAlert(String nodeId, Map<String, Object> appPayload) {
        Object dataObj = appPayload.get("data");
        if (!(dataObj instanceof Map<?, ?>))
            return;
        Map<String, Object> data = (Map<String, Object>) dataObj;
        Object envObj = data.get("environment");
        if (!(envObj instanceof Map<?, ?>))
            return;
        Map<String, Object> env = (Map<String, Object>) envObj;

        checkAndAlertEnv(nodeId, env);
    }

    /**
     * 直接传入环境 map 检查阈值。
     */
    public void checkAndAlertEnv(String nodeId, Map<String, ?> env) {
        List<NodeSensor> sensors = nodeSensorRepo.findByNodeId(nodeId);
        for (NodeSensor ns : sensors) {
            Object rawVal = env.get(ns.getSensorKey());
            if (rawVal == null)
                continue;
            double value = rawVal instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(rawVal));

            String sensorName = ns.getSensorKey();
            String sensorUnit = "";
            SensorType st = sensorTypeRepo.findBySensorKey(ns.getSensorKey()).orElse(null);
            if (st != null) {
                sensorName = st.getName();
                sensorUnit = st.getUnit() != null ? st.getUnit() : "";
            }

            String violation = null;
            if (ns.getWarningMax() != null && value > ns.getWarningMax()) {
                violation = String.format("节点%s%s %.1f%s 超过阈值 %.1f%s",
                        nodeId, sensorName, value, sensorUnit,
                        ns.getWarningMax(), sensorUnit);
            } else if (ns.getWarningMin() != null && value < ns.getWarningMin()) {
                violation = String.format("节点%s%s %.1f%s 低于阈值 %.1f%s",
                        nodeId, sensorName, value, sensorUnit,
                        ns.getWarningMin(), sensorUnit);
            }

            if (violation == null)
                continue;

            String cooldownKey = nodeId + ":" + ns.getSensorKey();
            long now = System.currentTimeMillis();
            Long last = lastAlertTime.get(cooldownKey);
            if (last != null && now - last < ALERT_COOLDOWN_MS)
                continue;
            lastAlertTime.put(cooldownKey, now);

            String alertId = "A" + alertSeq.incrementAndGet();
            String typeName = sensorName + "异常";

            alertService.addAlert(new AlertService.AlertInfo(
                    alertId, nodeId, ns.getSensorKey() + "_alert", typeName,
                    "warning", violation, ns.getSensorKey(), sensorName,
                    false, null, now));

            publishAlertMqtt(nodeId, alertId, ns.getSensorKey(), typeName, violation, sensorName, now);
        }
    }

    private void publishAlertMqtt(String nodeId, String alertId, String sensorKey,
            String typeName, String message, String sourceName, long timestamp) {
        Map<String, Object> alertData = new LinkedHashMap<>();
        alertData.put("alert_id", alertId);
        alertData.put("type", sensorKey + "_alert");
        alertData.put("type_name", typeName);
        alertData.put("level", "warning");
        alertData.put("timestamp", timestamp);
        alertData.put("message", message);
        alertData.put("source", sensorKey);
        alertData.put("source_name", sourceName);
        alertData.put("is_handled", false);
        alertData.put("handle_time", null);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msg_type", "alert");
        payload.put("node_id", nodeId);
        payload.put("timestamp", timestamp);
        payload.put("data", alertData);

        try {
            String topic = "/wangnong_2026/node/" + nodeId + "/alert";
            mqttService.publish(topic, MAPPER.writeValueAsString(payload), 1, false);
            log.info("[AlertCheck] Alert for {}: {}", nodeId, message);
        } catch (Exception e) {
            log.warn("[AlertCheck] Failed to publish alert", e);
        }
    }
}
