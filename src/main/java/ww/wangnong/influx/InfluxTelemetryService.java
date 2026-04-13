package ww.wangnong.influx;

import java.time.Instant;
import java.util.Map;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.write.WriteOptions;
import com.influxdb.v3.client.write.WritePrecision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * Purpose: 为 mock 数据生成与历史回填提供 InfluxDB 写入能力。
 * Responsibilities: 以 line protocol 形式写入环境指标和设备状态 measurement。
 * Flow: MockDataLifecycle/MockMqttSimulator 调用写入方法 -> 组装 line protocol -> 使用
 * InfluxDBClient 落库。
 * Dependencies: InfluxDBClient、hmht.influxdb.enabled 相关装配。
 * Input: deviceId、毫秒时间戳、环境指标或设备状态 map。
 * Output: InfluxDB 中的环境与设备状态时序数据。
 */
@Service
@ConditionalOnBean(InfluxDBClient.class)
public class InfluxTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(InfluxTelemetryService.class);

    private final InfluxDBClient client;

    public InfluxTelemetryService(InfluxDBClient client) {
        this.client = client;
    }

    /**
     * 写入环境传感器数据到 InfluxDB。
     *
     * @param deviceId  设备标识
     * @param timestamp MQTT 原始毫秒时间戳
     * @param env       环境数据 map (temperature, humidity, soilTemp, soilHumidity,
     *                  soilPh, light, co2)
     */
    public void writeEnvironment(String deviceId, long timestamp, Map<String, Double> env) {
        if (env == null || env.isEmpty()) {
            return;
        }

        StringBuilder lp = new StringBuilder("environment,device_id=")
                .append(escapeTag(deviceId));

        boolean hasField = false;
        for (Map.Entry<String, Double> entry : env.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            lp.append(hasField ? ',' : ' ');
            lp.append(escapeFieldKey(entry.getKey())).append('=').append(entry.getValue());
            hasField = true;
        }

        if (!hasField) {
            return;
        }

        lp.append(' ').append(Instant.ofEpochMilli(timestamp).toEpochMilli() * 1_000_000L);

        try {
            client.writeRecord(lp.toString(), new WriteOptions(null, WritePrecision.NS, null));
            log.trace("InfluxDB environment written: device={}", deviceId);
        } catch (Exception e) {
            log.error("InfluxDB write environment failed: device={}", deviceId, e);
        }
    }

    /**
     * 写入设备执行器状态到 InfluxDB。
     *
     * @param deviceId  设备标识
     * @param timestamp MQTT 原始毫秒时间戳
     * @param equipment 执行器状态 map (water_pump, led_light, fan, camera)
     * @param mode      运行模式 (smart / manual)
     */
    public void writeEquipmentState(String deviceId, long timestamp,
            Map<String, Boolean> equipment, String mode) {
        StringBuilder lp = new StringBuilder("equipment_state,device_id=")
                .append(escapeTag(deviceId));

        boolean hasField = false;
        if (equipment != null) {
            for (Map.Entry<String, Boolean> entry : equipment.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                lp.append(hasField ? ',' : ' ');
                lp.append(escapeFieldKey(entry.getKey())).append('=').append(entry.getValue());
                hasField = true;
            }
        }
        if (mode != null && !mode.isBlank()) {
            lp.append(hasField ? ',' : ' ');
            lp.append("mode=\"").append(mode.replace("\"", "\\\"")).append('"');
            hasField = true;
        }

        if (!hasField) {
            return;
        }

        lp.append(' ').append(Instant.ofEpochMilli(timestamp).toEpochMilli() * 1_000_000L);

        try {
            client.writeRecord(lp.toString(), new WriteOptions(null, WritePrecision.NS, null));
            log.trace("InfluxDB equipment_state written: device={}", deviceId);
        } catch (Exception e) {
            log.error("InfluxDB write equipment_state failed: device={}", deviceId, e);
        }
    }

    private String escapeTag(String value) {
        return value.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\=");
    }

    private String escapeFieldKey(String key) {
        return key.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\=");
    }
}
