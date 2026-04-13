package ww.wangnong.bridge;

import java.util.Map;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.write.WriteOptions;
import com.influxdb.v3.client.write.WritePrecision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;

/**
 * Purpose: 将已翻译的 App 格式遥测写入 InfluxDB。
 * Responsibilities: 把环境指标写入 environment/telemetry_metric，把设备状态写入
 * equipment_state，并兼顾新旧查询兼容。
 * Flow: MqttBridge/SimNodeSimulator 调用 writeTelemetry -> 提取环境和设备字段 -> 生成 line
 * protocol -> 写入 InfluxDB。
 * Dependencies: InfluxDBClient、TelemetryTranslator。
 * Input: nodeId、App 格式 telemetry payload。
 * Output: InfluxDB 中的时序记录。
 * Boundary: 允许 influxClient 为空时静默跳过，避免未启用时序库时影响主链路。
 */
@Component
public class InfluxTelemetryWriter {

    private static final Logger log = LoggerFactory.getLogger(InfluxTelemetryWriter.class);

    @Nullable
    private final InfluxDBClient influxClient;

    public InfluxTelemetryWriter(@Nullable InfluxDBClient influxClient) {
        this.influxClient = influxClient;
    }

    public void writeTelemetry(String nodeId, Map<String, Object> appPayload) {
        if (influxClient == null) {
            return;
        }
        try {
            WriteOptions opts = new WriteOptions(null, WritePrecision.MS, null);

            // Reason: 兼容历史查询仍依赖 environment measurement，同时为新查询补写 telemetry_metric 动态指标表。
            StringBuilder envLine = new StringBuilder("environment,node=").append(escape(nodeId)).append(' ');
            Map<String, Object> envData = TelemetryTranslator.extractEnvironment(appPayload);
            String source = resolveSource(appPayload);
            boolean hasField = false;
            for (var entry : envData.entrySet()) {
                if (entry.getValue() instanceof Number n) {
                    if (hasField)
                        envLine.append(',');
                    envLine.append(escape(entry.getKey())).append('=').append(n.doubleValue());
                    hasField = true;

                    String metricLine = "telemetry_metric,node=" + escape(nodeId)
                            + ",metric=" + escape(entry.getKey())
                            + ",source=" + escape(source)
                            + " value=" + n.doubleValue();
                    influxClient.writeRecord(metricLine, opts);
                }
            }
            if (hasField) {
                influxClient.writeRecord(envLine.toString(), opts);
            }

            Map<String, Object> eqData = TelemetryTranslator.extractEquipment(appPayload);
            for (var entry : eqData.entrySet()) {
                String eqName = entry.getKey();
                Object val = entry.getValue();
                String status = val instanceof Boolean b ? (b ? "on" : "off") : String.valueOf(val);
                String eqLine = "equipment_state,node=" + escape(nodeId) + ",equipment=" + escape(eqName)
                        + " status=\"" + status + "\"";
                influxClient.writeRecord(eqLine, opts);
            }
        } catch (Exception e) {
            log.warn("[InfluxWriter] Failed to write telemetry for node {}", nodeId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String resolveSource(Map<String, Object> appPayload) {
        Object dataObj = appPayload.get("data");
        if (dataObj instanceof Map<?, ?> data) {
            Object protocolVersion = ((Map<String, Object>) data).get("protocol_version");
            if (protocolVersion != null && !String.valueOf(protocolVersion).isBlank()) {
                return "hw_v2";
            }
        }
        return "agri_v1";
    }

    private static String escape(String tag) {
        return tag.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\=");
    }
}
