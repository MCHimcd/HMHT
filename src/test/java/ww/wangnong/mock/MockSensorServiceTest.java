package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import ww.wangnong.service.AlertService;
import ww.wangnong.service.SensorService.EquipmentStatsResult;
import ww.wangnong.service.SensorService.SensorHistoryResult;

class MockSensorServiceTest {

    private final AlertService alertService = mock(AlertService.class);
    private final MockSensorService service = new MockSensorService(alertService);

    @Test
    void getSensorHistory_default24h_returns48Points() {
        SensorHistoryResult result = service.getSensorHistory("N01", "temperature", "24h");

        assertThat(result.metric()).isEqualTo("temperature");
        assertThat(result.points()).hasSize(48);
        assertThat(result.summary().max()).isGreaterThanOrEqualTo(result.summary().min());
    }

    @Test
    void getSensorHistory_7d_returns42Points() {
        SensorHistoryResult result = service.getSensorHistory("N01", "humidity", "7d");

        assertThat(result.points()).hasSize(42);
    }

    @Test
    void getSensorHistory_refresh_appendsOnlyLatestPoint() {
        SensorHistoryResult first = service.getSensorHistory("N01", "humidity", "24h");
        SensorHistoryResult second = service.getSensorHistory("N01", "humidity", "24h");

        assertThat(second.points()).hasSize(48);
        assertThat(second.points().subList(0, 47))
                .containsExactlyElementsOf(first.points().subList(1, 48));
    }

    @Test
    void getEquipmentStats_returns7DayPoints() {
        EquipmentStatsResult result = service.getEquipmentStats("N01", "water_pump");

        assertThat(result.points()).hasSize(7);
        assertThat(result.summary()).containsKey("total_value");
        assertThat(result.summary()).containsKey("unit");
    }

    @Test
    void getSensorHistory_temperatureLowPoint_addsAlert() {
        service.getSensorHistory("SIM_Node_01", "temperature", "24h");
        service.getSensorHistory("SIM_Node_01", "temperature", "24h");
        service.getSensorHistory("SIM_Node_01", "temperature", "24h");
        SensorHistoryResult result = service.getSensorHistory("SIM_Node_01", "temperature", "24h");

        assertThat(result.points().getLast().value()).isLessThan(5.0);
        verify(alertService).addAlert(argThat(alert -> alert.alertId().startsWith("COURSE_LOW_TEMP_")
                && alert.nodeId().equals("SIM_Node_01")
                && alert.type().equals("temperature_low")
                && !alert.isHandled()));
    }
}
