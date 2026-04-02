package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ww.wangnong.service.SensorService.EquipmentStatsResult;
import ww.wangnong.service.SensorService.SensorHistoryResult;

class MockSensorServiceTest {

    private final MockSensorService service = new MockSensorService();

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
    void getEquipmentStats_returns7DayPoints() {
        EquipmentStatsResult result = service.getEquipmentStats("N01", "water_pump");

        assertThat(result.points()).hasSize(7);
        assertThat(result.summary()).containsKey("total_value");
        assertThat(result.summary()).containsKey("unit");
    }
}
