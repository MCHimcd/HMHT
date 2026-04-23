package ww.wangnong.service;

import java.util.List;
import java.util.Map;

/**
 * Purpose: 定义传感器历史与设备统计查询契约。
 * Responsibilities: 统一时序历史点、统计摘要和设备开关统计返回结构。
 * Flow: 控制器提供节点与指标参数 -> 实现类生成课程版模拟曲线 -> 返回序列化友好的 record。
 * Dependencies: MockSensorService。
 * Input: nodeId、metric、range、equipmentKey。
 * Output: 传感器历史序列和设备统计结果。
 */
public interface SensorService {

    record HistoryPoint(String timeLabel, double value) {
    }

    record HistorySummary(double max, double min, double avg) {
    }

    record SensorHistoryResult(String metric, HistorySummary summary,
            List<HistoryPoint> points) {
    }

    record StatsPoint(String timeLabel, double value) {
    }

    record EquipmentStatsResult(Map<String, Object> summary, List<StatsPoint> points) {
    }

    SensorHistoryResult getSensorHistory(String nodeId, String metric, String range);

    EquipmentStatsResult getEquipmentStats(String nodeId, String equipmentKey);
}
