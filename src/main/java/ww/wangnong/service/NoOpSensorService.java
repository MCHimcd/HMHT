package ww.wangnong.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ww.wangnong.config.condition.ConditionalOnBridgeMode;

/**
 * Fallback SensorService for bridge/hardware/full modes. Returns empty results
 * so that SensorController can always wire up. When InfluxSensorService is also
 * present it takes priority via {@code @Primary}.
 */
@Service
@ConditionalOnBridgeMode
class NoOpSensorService implements SensorService {

    @Override
    public SensorHistoryResult getSensorHistory(String nodeId, String metric, String range) {
        return new SensorHistoryResult(metric, new HistorySummary(0, 0, 0), List.of());
    }

    @Override
    public EquipmentStatsResult getEquipmentStats(String nodeId, String equipmentKey) {
        return new EquipmentStatsResult(Map.of(), List.of());
    }
}
