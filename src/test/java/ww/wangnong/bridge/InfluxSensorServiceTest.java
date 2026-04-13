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

    @SuppressWarnings("null")
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

    @SuppressWarnings("null")
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

    @SuppressWarnings("null")
    @Test
    void getSensorHistory_bothQueriesEmpty_returnsFallbackData() {
        InfluxSensorService service = new InfluxSensorService(influxClient);

        // Both new and legacy queries return empty streams (count=0)
        when(influxClient.query(any(String.class), any(QueryOptions.class)))
                .thenReturn(Stream.empty());

        var result = service.getSensorHistory("SIM_Node_01", "temperature", "24h");

        assertThat(result.metric()).isEqualTo("temperature");
        assertThat(result.points()).isNotEmpty();
        assertThat(result.summary().max()).isGreaterThan(0);
        assertThat(result.summary().avg()).isGreaterThan(0);
    }

    @SuppressWarnings("null")
    @Test
    void getSensorHistory_bothQueriesFail_returnsFallbackData() {
        InfluxSensorService service = new InfluxSensorService(influxClient);

        // Both queries throw exceptions (InfluxDB unreachable)
        when(influxClient.query(any(String.class), any(QueryOptions.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        var result = service.getSensorHistory("SIM_Node_01", "humidity", "7d");

        assertThat(result.metric()).isEqualTo("humidity");
        assertThat(result.points()).isNotEmpty();
        assertThat(result.summary().max()).isGreaterThan(0);
    }

    @SuppressWarnings("null")
    @Test
    void getSensorHistory_queryBlocks_timesOutAndReturnsFallback() {
        InfluxSensorService service = new InfluxSensorService(influxClient);

        // Simulate a blocking InfluxDB query (gRPC hang)
        when(influxClient.query(any(String.class), any(QueryOptions.class)))
                .thenAnswer(inv -> {
                    Thread.sleep(5000); // block longer than QUERY_TIMEOUT_MS
                    return Stream.empty();
                });

        long start = System.currentTimeMillis();
        var result = service.getSensorHistory("SIM_Node_01", "temperature", "24h");
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.metric()).isEqualTo("temperature");
        assertThat(result.points()).isNotEmpty();
        // Should return within ~2s, not 5s+
        assertThat(elapsed).isLessThan(3000);
    }

    @SuppressWarnings("null")
    @Test
    void getSensorHistory_circuitOpensAfterFailure_subsequentCallsReturnFallbackImmediately() {
        InfluxSensorService service = new InfluxSensorService(influxClient);

        // First call: InfluxDB throws exception
        when(influxClient.query(any(String.class), any(QueryOptions.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        var result1 = service.getSensorHistory("SIM_Node_01", "temperature", "24h");
        assertThat(result1.points()).isNotEmpty();

        // Second call should return immediately without querying InfluxDB (circuit
        // open)
        long start = System.currentTimeMillis();
        var result2 = service.getSensorHistory("SIM_Node_01", "humidity", "24h");
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result2.points()).isNotEmpty();
        assertThat(elapsed).isLessThan(100); // Near-instant due to circuit breaker
    }

    @Test
    void generateFallbackHistory_24h_returns48Points() {
        InfluxSensorService service = new InfluxSensorService(influxClient);
        var result = service.generateFallbackHistory("temperature", "24h");
        assertThat(result.points()).hasSize(48);
        assertThat(result.metric()).isEqualTo("temperature");
        assertThat(result.summary().max()).isGreaterThanOrEqualTo(result.summary().min());
    }

    @Test
    void generateFallbackHistory_7d_returns42Points() {
        InfluxSensorService service = new InfluxSensorService(influxClient);
        var result = service.generateFallbackHistory("humidity", "7d");
        assertThat(result.points()).hasSize(42);
    }

    @Test
    void generateFallbackHistory_30d_returns30Points() {
        InfluxSensorService service = new InfluxSensorService(influxClient);
        var result = service.generateFallbackHistory("co2", "30d");
        assertThat(result.points()).hasSize(30);
    }

    @SuppressWarnings("null")
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
