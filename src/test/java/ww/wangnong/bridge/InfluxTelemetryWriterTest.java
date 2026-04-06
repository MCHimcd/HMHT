package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.influxdb.v3.client.InfluxDBClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InfluxTelemetryWriterTest {

    @Mock
    private InfluxDBClient influxClient;

    @Test
    void writeTelemetry_hasEnvironmentAndEquipment_writesTelemetryMetricAndEquipmentState() {
        InfluxTelemetryWriter writer = new InfluxTelemetryWriter(influxClient);

        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("temperature", 25.3);
        environment.put("humidity", 61.2);
        environment.put("mode", "auto");

        Map<String, Object> equipment = new LinkedHashMap<>();
        equipment.put("water_pump", true);

        Map<String, Object> payload = Map.of(
                "msg_type", "telemetry",
                "node_id", "N01",
                "data", Map.of("environment", environment, "equipment", equipment, "protocol_version", "2.0"));

        writer.writeTelemetry("N01", payload);

        ArgumentCaptor<String> lineCaptor = ArgumentCaptor.forClass(String.class);
        verify(influxClient, atLeastOnce()).writeRecord(lineCaptor.capture(), any());

        List<String> lines = lineCaptor.getAllValues();
        assertThat(lines).anyMatch(line -> line.contains("telemetry_metric,node=N01")
            && line.contains("metric=temperature")
            && line.contains("value=25.3"));
        assertThat(lines).anyMatch(line -> line.contains("telemetry_metric,node=N01")
            && line.contains("metric=humidity")
            && line.contains("value=61.2"));
        assertThat(lines).anyMatch(line -> line.contains("equipment_state,node=N01,equipment=water_pump")
            && line.contains("status=\"on\""));
    }

    @Test
    void writeTelemetry_noNumericEnvironment_skipsTelemetryMetric() {
        InfluxTelemetryWriter writer = new InfluxTelemetryWriter(influxClient);

        Map<String, Object> payload = Map.of(
                "data", Map.of("environment", Map.of("mode", "manual"), "equipment", Map.of()));

        writer.writeTelemetry("N01", payload);

        verify(influxClient, never()).writeRecord(org.mockito.ArgumentMatchers.contains("telemetry_metric"), any());
    }

    @Test
    void writeTelemetry_nullClient_returnsWithoutError() {
        InfluxTelemetryWriter writer = new InfluxTelemetryWriter(null);
        writer.writeTelemetry("N01", Map.of("data", Map.of()));
    }
}
