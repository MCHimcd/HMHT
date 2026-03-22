package ww.wangnong.mock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ww.wangnong.mqtt.PahoMqttService;
import ww.wangnong.repository.AlertRepository;
import ww.wangnong.repository.AlertRepository;
import ww.wangnong.service.AlertService.AlertInfo;

@Component
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "mock", matchIfMissing = true)
public class MockAlertGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockAlertGenerator.class);
    private static final Random RNG = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String[] NODE_IDS = {"A03_Node_01", "A03_Node_02"};
    private static final Map<String, String> NODE_NAMES = Map.of(
            "A03_Node_01", "温室节点 1",
            "A03_Node_02", "温室节点 2");

    private final MockAlertService alertService;
    private final AlertRepository alertRepo;
    private final PahoMqttService mqttService;

    public MockAlertGenerator(MockAlertService alertService, AlertRepository alertRepo,
                              PahoMqttService mqttService) {
        this.alertService = alertService;
        this.alertRepo = alertRepo;
        this.mqttService = mqttService;
    }

    @Scheduled(fixedDelay = 15_000, initialDelay = 5_000)
    public void generate() {
        // 未处理告警 >= 5 则停止生成
        long unhandled = alertRepo.findTop10ByIsHandledOrderByCreatedAtDesc(false).size();
        if (unhandled >= 5) return;

        String nodeId = NODE_IDS[RNG.nextInt(NODE_IDS.length)];
        String nodeName = NODE_NAMES.getOrDefault(nodeId, nodeId);
        long now = System.currentTimeMillis();
        String alertId = "A" + (10000 + RNG.nextInt(90000));

        int tplIdx = RNG.nextInt(5);
        String type, typeName, level, source, sourceName, message;
        switch (tplIdx) {
            case 0 -> {
                type = "temp_high"; typeName = "高温异常"; level = "critical";
                source = "environment"; sourceName = "环境监测";
                message = String.format("节点%s气温升至%.1f°C，建议立即通风降温。",
                        nodeName, 33.0 + RNG.nextDouble() * 5.5);
            }
            case 1 -> {
                type = "humidity_low"; typeName = "空气湿度过低"; level = "warning";
                source = "environment"; sourceName = "环境监测";
                message = String.format("节点%s空气湿度降至%.1f%%，建议及时补水。",
                        nodeName, 35.0 + RNG.nextDouble() * 10.0);
            }
            case 2 -> {
                type = "soil_moisture_low"; typeName = "土壤湿度偏低"; level = "warning";
                source = "environment"; sourceName = "环境监测";
                message = String.format("节点%s土壤湿度仅%.1f%%，需检查灌溉系统。",
                        nodeName, 18.0 + RNG.nextDouble() * 10.0);
            }
            case 3 -> {
                type = "pest_warning"; typeName = "病害预警"; level = "warning";
                source = "video"; sourceName = "视频AI";
                message = String.format("节点%s画面疑似识别到叶片病斑，请尽快复核。", nodeName);
            }
            default -> {
                type = "intrusion_warning"; typeName = "异常闯入预警"; level = "critical";
                source = "video"; sourceName = "视频AI";
                message = String.format("节点%s监控区域检测到异常闯入，请及时确认现场情况。", nodeName);
            }
        }

        AlertInfo alert = new AlertInfo(alertId, nodeId, type, typeName, level,
                message, source, sourceName, false, null, now);
        alertService.addAlert(alert);

        if (mqttService.isConnected()) {
            try {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("alert_id", alertId);
                data.put("type", type);
                data.put("type_name", typeName);
                data.put("level", level);
                data.put("timestamp", now);
                data.put("message", message);
                data.put("source", source);
                data.put("source_name", sourceName);
                data.put("is_handled", false);
                data.put("handle_time", null);

                Map<String, Object> payload = Map.of(
                        "msg_type", "alert",
                        "node_id", nodeId,
                        "timestamp", now,
                        "data", data);
                mqttService.publish("/wangnong_2026/node/" + nodeId + "/alert",
                        MAPPER.writeValueAsString(payload), 1, false);
            } catch (Exception e) {
                log.warn("[MockAlert] Failed to publish alert via MQTT", e);
            }
        }
    }
}
