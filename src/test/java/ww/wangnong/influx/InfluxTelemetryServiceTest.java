package ww.wangnong.influx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.write.WriteOptions;

@ExtendWith(MockitoExtension.class)
class InfluxTelemetryServiceTest {

    @Mock
    private InfluxDBClient client;

    private InfluxTelemetryService service;

    @BeforeEach
    void setUp() {
        service = new InfluxTelemetryService(client);
    }

    @Test
    void writeEnvironment_validData_writesLineProtocol() {
        service.writeEnvironment("D01", 1700000000000L,
                Map.of("temperature", 25.5, "humidity", 60.0));

        verify(client).writeRecord(
                argThat(lp -> lp.startsWith("environment,device_id=D01 ")
                        && lp.contains("temperature=25.5")
                        && lp.contains("humidity=60.0")),
                any(WriteOptions.class));
    }

    @Test
    void writeEnvironment_emptyMap_skipsWrite() {
        service.writeEnvironment("D01", 1700000000000L, Map.of());

        verify(client, never()).writeRecord(any(String.class), any(WriteOptions.class));
    }

    @Test
    void writeEnvironment_nullMap_skipsWrite() {
        service.writeEnvironment("D01", 1700000000000L, null);

        verify(client, never()).writeRecord(any(String.class), any(WriteOptions.class));
    }

    @Test
    void writeEquipmentState_validData_writesLineProtocol() {
        service.writeEquipmentState("D01", 1700000000000L,
                Map.of("water_pump", true, "led_light", false), "smart");

        verify(client).writeRecord(
                argThat(lp -> lp.startsWith("equipment_state,device_id=D01 ")
                        && lp.contains("water_pump=true")
                        && lp.contains("led_light=false")
                        && lp.contains("mode=\"smart\"")),
                any(WriteOptions.class));
    }
}
