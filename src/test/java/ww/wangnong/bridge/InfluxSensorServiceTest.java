package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.stream.Stream;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.query.QueryOptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InfluxSensorServiceTest {

    @Mock
    private InfluxDBClient influxClient;

    @Test
    void getSensorHistory_metricQuery_usesTelemetryMetricMeasurement() {
        InfluxSensorService service = new InfluxSensorService(influxClient);

        when(influxClient.query(any(String.class), any(QueryOptions.class)))
            .thenReturn(Stream.<Object[]>of(new Object[] { Instant.parse("2026-04-06T00:00:00Z"), 24.6 }));

        var result = service.getSensorHistory("N01", "temperature", "24h");

        assertThat(result.metric()).isEqualTo("temperature");
        assertThat(result.points()).hasSize(1);
        assertThat(result.summary().avg()).isEqualTo(24.6);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(influxClient).query(sqlCaptor.capture(), any(QueryOptions.class));

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("FROM telemetry_metric");
        assertThat(sql).contains("node = 'N01'");
        assertThat(sql).contains("metric = 'temperature'");
    }

    @Test
    void getSensorHistory_newMeasurementEmpty_fallsBackToLegacyEnvironment() {
        InfluxSensorService service = new InfluxSensorService(influxClient);

        when(influxClient.query(any(String.class), any(QueryOptions.class)))
                .thenReturn(Stream.empty())
            .thenReturn(Stream.<Object[]>of(new Object[] { Instant.parse("2026-04-06T01:00:00Z"), 21.1 }));

        var result = service.getSensorHistory("N01", "temperature", "24h");

        assertThat(result.points()).hasSize(1);
        assertThat(result.summary().max()).isEqualTo(21.1);
        verify(influxClient, times(2)).query(any(String.class), any(QueryOptions.class));
    }

    @Test
    void getEquipmentStats_queryStillUsesEquipmentState() {
        InfluxSensorService service = new InfluxSensorService(influxClient);

        when(influxClient.query(any(String.class), any(QueryOptions.class)))
            .thenReturn(Stream.<Object[]>of(new Object[] { Instant.parse("2026-04-06T00:00:00Z"), 2.0 }));

        var result = service.getEquipmentStats("N01", "water_pump");
        assertThat(result.summary()).containsEntry("unit", "灌溉量(升)");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(influxClient).query(sqlCaptor.capture(), any(QueryOptions.class));
        assertThat(sqlCaptor.getValue()).contains("FROM equipment_state");
    }
}
