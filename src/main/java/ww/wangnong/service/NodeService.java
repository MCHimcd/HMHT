package ww.wangnong.service;

import java.util.List;
import java.util.Map;

/**
 * Purpose: 定义节点查询与节点元数据管理契约。
 * Responsibilities: 提供节点列表、节点详情、传感器/设备元数据和阈值修改入口。
 * Flow: 控制器传入 token 或 nodeId -> 实现类完成权限校验、元数据聚合和阈值更新 -> 返回节点视图。
 * Dependencies: 节点、用户授权、能力快照与全局类型定义。
 * Input: token、nodeId、阈值更新列表。
 * Output: 节点摘要、节点详情、元数据列表或空成功结果。
 */
public interface NodeService {

        record NodeInfo(String id, String name, String status) {
        }

        record NodeDetail(String id, String name, String status,
                        Map<String, Object> nodeState) {
        }

        record SensorMetaInfo(String key, String name, String unit, String icon,
                        String badgeColor, String bgColor,
                        Double warningMin, Double warningMax) {
        }

        record EquipmentMetaInfo(String key, String name, String icon, boolean controllable,
                        String controlType) {
        }

        List<NodeInfo> listNodes(String token);

        NodeDetail getNode(String token, String nodeId);

        List<SensorMetaInfo> getSensorMeta(String nodeId);

        List<EquipmentMetaInfo> getEquipmentMeta(String nodeId);

        record ThresholdUpdate(String sensorKey, Double warningMin, Double warningMax) {
        }

        void updateSensorThresholds(String token, String nodeId, List<ThresholdUpdate> thresholds);
}
