package ww.wangnong.service;

import java.util.List;
import java.util.Map;

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

    record EquipmentMetaInfo(String key, String name, String icon, boolean controllable) {
    }

    List<NodeInfo> listNodes(String token);

    NodeDetail getNode(String token, String nodeId);

    List<SensorMetaInfo> getSensorMeta(String nodeId);

    List<EquipmentMetaInfo> getEquipmentMeta(String nodeId);

    record ThresholdUpdate(String sensorKey, Double warningMin, Double warningMax) {
    }

    void updateSensorThresholds(String token, String nodeId, List<ThresholdUpdate> thresholds);
}
