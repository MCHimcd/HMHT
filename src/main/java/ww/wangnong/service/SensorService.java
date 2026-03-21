package ww.wangnong.service;

import java.util.List;
import java.util.Map;

public interface SensorService {

    record HistoryPoint(String timeLabel, double value) {}

    record HistorySummary(double max, double min, double avg) {}

    record SensorHistoryResult(String metric, HistorySummary summary,
                               List<HistoryPoint> points) {}

    record StatsPoint(String timeLabel, double value) {}

    record EquipmentStatsResult(Map<String, Object> summary, List<StatsPoint> points) {}

    SensorHistoryResult getSensorHistory(String deviceId, String metric, String range);

    EquipmentStatsResult getEquipmentStats(String deviceId, String equipmentKey);
}
