package ww.wangnong.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ww.wangnong.config.condition.ConditionalOnBridgeMode;

/**
 * Purpose: 为 bridge/hardware/full 模式提供兜底 SensorService。
 * Responsibilities: 在 InfluxSensorService 未装配时仍满足控制器注入需求，并返回空结果避免启动失败。
 * Flow: SensorController 调用 -> 直接返回空历史或空统计 -> 若存在 @Primary 的
 * InfluxSensorService 则由后者接管。
 * Dependencies: 条件装配 @ConditionalOnBridgeMode。
 * Input: nodeId、metric、equipmentKey。
 * Output: 空的传感器历史与设备统计结果。
 * Boundary: 这里只保证 Bean 存在，不提供真实数据语义。
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
