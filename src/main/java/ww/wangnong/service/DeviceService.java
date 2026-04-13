package ww.wangnong.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Purpose: 定义设备能力快照与设备影子查询契约。
 * Responsibilities: 固定控制面返回结构，确保控制器与桥接持久化解耦。
 * Flow: 控制器按 deviceId 发起查询 -> 实现类聚合能力明细或 shadow JSON -> 返回 record 结果。
 * Dependencies: 控制面 capability/shadow 持久化实现。
 * Input: deviceId。
 * Output: capability 结果、shadow 结果及其嵌套明细。
 */
public interface DeviceService {

        record SensorCapability(
                        String sensorKey,
                        String dataType,
                        String unit,
                        Double minValue,
                        Double maxValue,
                        Boolean requiredField) {
        }

        record EquipmentCapability(
                        String equipmentKey,
                        Boolean controllable,
                        List<String> actions) {
        }

        record DeviceCapabilityResult(
                        String deviceId,
                        String capabilityVersion,
                        String checksum,
                        LocalDateTime reportedAt,
                        List<SensorCapability> sensors,
                        List<EquipmentCapability> equipment) {
        }

        record DeviceShadowResult(
                        String deviceId,
                        Map<String, Object> shadowState,
                        LocalDateTime updatedAt) {
        }

        DeviceCapabilityResult getDeviceCapability(String deviceId);

        DeviceShadowResult getDeviceShadow(String deviceId);
}
