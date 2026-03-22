package ww.wangnong.bridge;

import java.util.Map;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.write.WriteOptions;
import com.influxdb.v3.client.write.WritePrecision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Writes translated telemetry data to InfluxDB.
 */
@Component
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "bridge")
public class InfluxTelemetryWriter {

    private static final Logger log = LoggerFactory.getLogger(InfluxTelemetryWriter.class);

    @Nullable
    private final InfluxDBClient influxClient;

    public InfluxTelemetryWriter(@Nullable InfluxDBClient influxClient) {
        this.influxClient = influxClient;
    }

    /**
     * Write a translated App-format telemetry payload to InfluxDB.
     * Environment fields go to "environment" measurement,
     * equipment states go to "equipment_state" measurement.
     */
    public void writeTelemetry(String nodeId, Map<String, Object> appPayload) {
        if (influxClient == null) {
            return;
        }
        try {
            StringBuilder envLine = new StringBuilder("environment,node=").append(escape(nodeId)).append(' ');
            Map<String, Object> envData = TelemetryTranslator.extractEnvironment(appPayload);
            boolean hasField = false;
            for (var entry : envData.entrySet()) {
                if (entry.getValue() instanceof Number n) {
                    if (hasField) envLine.append(',');
                    envLine.append(escape(entry.getKey())).append('=').append(n.doubleValue());
                    hasField = true;
                }
            }
            if (hasField) {
                WriteOptions opts = new WriteOptions(null, WritePrecision.MS, null);
                influxClient.writeRecord(envLine.toString(), opts);
            }

            Map<String, Object> eqData = TelemetryTranslator.extractEquipment(appPayload);
            for (var entry : eqData.entrySet()) {
                String eqName = entry.getKey();
                Object val = entry.getValue();
                String status = val instanceof Boolean b ? (b ? "on" : "off") : String.valueOf(val);
                String eqLine = "equipment_state,node=" + escape(nodeId) + ",equipment=" + escape(eqName)
                        + " status=\"" + status + "\"";
                WriteOptions opts = new WriteOptions(null, WritePrecision.MS, null);
                influxClient.writeRecord(eqLine, opts);
            }
        } catch (Exception e) {
            log.warn("[InfluxWriter] Failed to write telemetry for node {}", nodeId, e);
        }
    }

    private static String escape(String tag) {
        return tag.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\=");
    }
}
