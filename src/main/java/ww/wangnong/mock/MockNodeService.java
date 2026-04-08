package ww.wangnong.mock;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.CapabilityEquipment;
import ww.wangnong.entity.CapabilitySensor;
import ww.wangnong.entity.DeviceCapability;
import ww.wangnong.entity.DeviceProfile;
import ww.wangnong.entity.EquipmentType;
import ww.wangnong.entity.SensorType;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.CapabilityEquipmentRepository;
import ww.wangnong.repository.CapabilitySensorRepository;
import ww.wangnong.repository.DeviceCapabilityRepository;
import ww.wangnong.repository.DeviceProfileRepository;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.repository.UserNodeAccessRepository;
import ww.wangnong.service.NodeService;
import ww.wangnong.service.OperationLogService;

@Service
public class MockNodeService implements NodeService {

    private static final long HEARTBEAT_TIMEOUT_SECONDS = 60;
    private static final String DEFAULT_SENSOR_ICON = "sensor";
    private static final String DEFAULT_SENSOR_BADGE_COLOR = "#90A4AE";
    private static final String DEFAULT_SENSOR_BG_COLOR = "#ECEFF1";
    private static final String DEFAULT_EQUIPMENT_ICON = "device";

    private final MockAuthService authService;
    private final NodeRepository nodeRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final NodeSensorRepository nodeSensorRepo;
    private final EquipmentTypeRepository equipmentTypeRepo;
    private final NodeEquipmentRepository nodeEquipmentRepo;
    private final UserNodeAccessRepository userNodeAccessRepo;
    private final DeviceCapabilityRepository deviceCapabilityRepository;
    private final CapabilitySensorRepository capabilitySensorRepository;
    private final CapabilityEquipmentRepository capabilityEquipmentRepository;
    private final DeviceProfileRepository deviceProfileRepository;
    private final OperationLogService opLog;

    public MockNodeService(MockAuthService authService,
            NodeRepository nodeRepo,
            SensorTypeRepository sensorTypeRepo,
            NodeSensorRepository nodeSensorRepo,
            EquipmentTypeRepository equipmentTypeRepo,
            NodeEquipmentRepository nodeEquipmentRepo,
            UserNodeAccessRepository userNodeAccessRepo,
            DeviceCapabilityRepository deviceCapabilityRepository,
            CapabilitySensorRepository capabilitySensorRepository,
            CapabilityEquipmentRepository capabilityEquipmentRepository,
            DeviceProfileRepository deviceProfileRepository,
            OperationLogService opLog) {
        this.authService = authService;
        this.nodeRepo = nodeRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.nodeSensorRepo = nodeSensorRepo;
        this.equipmentTypeRepo = equipmentTypeRepo;
        this.nodeEquipmentRepo = nodeEquipmentRepo;
        this.userNodeAccessRepo = userNodeAccessRepo;
        this.deviceCapabilityRepository = deviceCapabilityRepository;
        this.capabilitySensorRepository = capabilitySensorRepository;
        this.capabilityEquipmentRepository = capabilityEquipmentRepository;
        this.deviceProfileRepository = deviceProfileRepository;
        this.opLog = opLog;
    }

    @Override
    public List<NodeInfo> listNodes(String token) {
        User user = authService.requireUser(token);
        List<Node> nodes;
        if ("admin".equals(user.getRole())) {
            nodes = nodeRepo.findAll();
        } else {
            // 拥有的节点 + 被授权的节点
            List<Node> owned = nodeRepo.findByUserId(user.getId());
            List<String> accessNodeIds = userNodeAccessRepo.findByUserId(user.getId())
                    .stream().map(UserNodeAccess::getNodeId).toList();
            List<Node> accessed = accessNodeIds.isEmpty() ? List.of() : nodeRepo.findAllByNodeIdIn(accessNodeIds);
            Map<String, Node> deduplicated = new LinkedHashMap<>();
            Stream.concat(owned.stream(), accessed.stream())
                    .forEach(node -> deduplicated.putIfAbsent(node.getNodeId(), node));
            nodes = List.copyOf(deduplicated.values());
        }
        return nodes.stream()
                .map(n -> new NodeInfo(n.getNodeId(), n.getNodeName(),
                        resolveNodeStatus(n.getNodeId(), n.getOnline())))
                .toList();
    }

    @Override
    public NodeDetail getNode(String token, String nodeId) {
        User user = authService.requireUser(token);
        Node n = nodeRepo.findByNodeId(nodeId)
                .orElseThrow(() -> new BizException(404, "节点不存在"));
        if (!"admin".equals(user.getRole()) && !n.getUser().getId().equals(user.getId())
                && !userNodeAccessRepo.existsByUserIdAndNodeId(user.getId(), nodeId)) {
            throw new BizException(403, "无权访问该节点");
        }
        return new NodeDetail(n.getNodeId(), n.getNodeName(),
                resolveNodeStatus(n.getNodeId(), n.getOnline()),
                Map.of("smartMode", n.getSmartMode()));
    }

    @Override
    public List<SensorMetaInfo> getSensorMeta(String nodeId) {
        List<SensorMetaInfo> capabilityMeta = getCapabilitySensorMeta(nodeId);
        if (!capabilityMeta.isEmpty()) {
            return capabilityMeta;
        }

        List<NodeSensor> bindings = nodeSensorRepo.findByNodeId(nodeId);
        return bindings.stream().map(ns -> {
            var type = sensorTypeRepo.findBySensorKey(ns.getSensorKey()).orElse(null);
            if (type == null)
                return null;
            return new SensorMetaInfo(type.getSensorKey(), type.getName(), type.getUnit(),
                    type.getIcon(), type.getBadgeColor(), type.getBgColor(),
                    ns.getWarningMin(), ns.getWarningMax());
        }).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public List<EquipmentMetaInfo> getEquipmentMeta(String nodeId) {
        List<EquipmentMetaInfo> capabilityMeta = getCapabilityEquipmentMeta(nodeId);
        if (!capabilityMeta.isEmpty()) {
            return capabilityMeta;
        }

        List<NodeEquipment> bindings = nodeEquipmentRepo.findByNodeId(nodeId);
        return bindings.stream().map(ne -> {
            var type = equipmentTypeRepo.findByEquipmentKey(ne.getEquipmentKey()).orElse(null);
            if (type == null)
                return null;
            return new EquipmentMetaInfo(type.getEquipmentKey(), type.getName(),
                    type.getIcon(), type.getControllable());
        }).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public void updateSensorThresholds(String token, String nodeId, List<ThresholdUpdate> thresholds) {
        User user = authService.requireUser(token);
        nodeRepo.findByNodeId(nodeId)
                .orElseThrow(() -> new BizException(404, "节点不存在"));
        for (ThresholdUpdate tu : thresholds) {
            NodeSensor ns = nodeSensorRepo.findByNodeIdAndSensorKey(nodeId, tu.sensorKey())
                    .orElseThrow(() -> new BizException(404, "传感器不存在: " + tu.sensorKey()));
            ns.setWarningMin(tu.warningMin());
            ns.setWarningMax(tu.warningMax());
            nodeSensorRepo.save(ns);
        }
        opLog.log("THRESHOLD_UPDATE", user.getId(), user.getUserName(),
                "node", nodeId, thresholds.size() + " 传感器阈值已更新");
    }

    private List<SensorMetaInfo> getCapabilitySensorMeta(String nodeId) {
        Optional<DeviceCapability> capabilityOpt = deviceCapabilityRepository
                .findTopByDeviceIdOrderByCreatedAtDesc(nodeId);
        if (capabilityOpt == null || capabilityOpt.isEmpty()) {
            return List.of();
        }

        DeviceCapability capability = capabilityOpt.get();
        List<CapabilitySensor> sensors = capabilitySensorRepository
                .findByDeviceIdAndCapabilityVersionOrderBySensorKey(nodeId, capability.getCapabilityVersion());
        if (sensors == null || sensors.isEmpty()) {
            return List.of();
        }

        Map<String, NodeSensor> thresholdByKey = new LinkedHashMap<>();
        List<NodeSensor> thresholdList = nodeSensorRepo.findByNodeId(nodeId);
        if (thresholdList != null) {
            for (NodeSensor threshold : thresholdList) {
                if (threshold.getSensorKey() != null && !threshold.getSensorKey().isBlank()) {
                    thresholdByKey.put(threshold.getSensorKey(), threshold);
                }
            }
        }

        Map<String, SensorMetaInfo> deduplicated = new LinkedHashMap<>();
        for (CapabilitySensor sensor : sensors) {
            String rawKey = sensor.getSensorKey();
            String canonicalKey = toAppSensorKey(rawKey);
            if (canonicalKey.isBlank()) {
                continue;
            }

            SensorType type = sensorTypeRepo.findBySensorKey(canonicalKey).orElse(null);
            NodeSensor threshold = thresholdByKey.containsKey(canonicalKey)
                    ? thresholdByKey.get(canonicalKey)
                    : thresholdByKey.get(rawKey);

            String name = type != null && type.getName() != null && !type.getName().isBlank()
                    ? type.getName()
                    : canonicalKey;
            String unit = type != null && type.getUnit() != null && !type.getUnit().isBlank()
                    ? type.getUnit()
                    : defaultIfBlank(sensor.getUnit(), "");
            String icon = type != null && type.getIcon() != null && !type.getIcon().isBlank()
                    ? type.getIcon()
                    : DEFAULT_SENSOR_ICON;
            String badgeColor = type != null && type.getBadgeColor() != null && !type.getBadgeColor().isBlank()
                    ? type.getBadgeColor()
                    : DEFAULT_SENSOR_BADGE_COLOR;
            String bgColor = type != null && type.getBgColor() != null && !type.getBgColor().isBlank()
                    ? type.getBgColor()
                    : DEFAULT_SENSOR_BG_COLOR;

            Double warningMin = threshold == null ? null : threshold.getWarningMin();
            Double warningMax = threshold == null ? null : threshold.getWarningMax();

            deduplicated.putIfAbsent(canonicalKey,
                    new SensorMetaInfo(canonicalKey, name, unit, icon, badgeColor, bgColor, warningMin, warningMax));
        }

        return List.copyOf(deduplicated.values());
    }

    private List<EquipmentMetaInfo> getCapabilityEquipmentMeta(String nodeId) {
        Optional<DeviceCapability> capabilityOpt = deviceCapabilityRepository
                .findTopByDeviceIdOrderByCreatedAtDesc(nodeId);
        if (capabilityOpt == null || capabilityOpt.isEmpty()) {
            return List.of();
        }

        DeviceCapability capability = capabilityOpt.get();
        List<CapabilityEquipment> equipments = capabilityEquipmentRepository
                .findByDeviceIdAndCapabilityVersionOrderByEquipmentKey(nodeId, capability.getCapabilityVersion());
        if (equipments == null || equipments.isEmpty()) {
            return List.of();
        }

        return equipments.stream().map(equipment -> {
            String key = defaultIfBlank(equipment.getEquipmentKey(), "");
            if (key.isBlank()) {
                return null;
            }

            EquipmentType type = equipmentTypeRepo.findByEquipmentKey(key).orElse(null);
            String name = type != null && type.getName() != null && !type.getName().isBlank()
                    ? type.getName()
                    : key;
            String icon = type != null && type.getIcon() != null && !type.getIcon().isBlank()
                    ? type.getIcon()
                    : DEFAULT_EQUIPMENT_ICON;

            boolean controllable;
            if (equipment.getControllable() != null) {
                controllable = equipment.getControllable();
            } else if (type != null && type.getControllable() != null) {
                controllable = type.getControllable();
            } else {
                controllable = true;
            }

            return new EquipmentMetaInfo(key, name, icon, controllable);
        }).filter(java.util.Objects::nonNull).toList();
    }

    private String resolveNodeStatus(String nodeId, Boolean fallbackOnline) {
        Optional<DeviceProfile> profileOpt = deviceProfileRepository.findByDeviceId(nodeId);
        if (profileOpt == null || profileOpt.isEmpty()) {
            return Boolean.TRUE.equals(fallbackOnline) ? "online" : "offline";
        }

        DeviceProfile profile = profileOpt.get();
        if (!Boolean.TRUE.equals(profile.getOnline())) {
            return "offline";
        }

        LocalDateTime lastSeen = profile.getLastSeenAt();
        if (lastSeen == null) {
            return "offline";
        }

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        return lastSeen.isBefore(threshold) ? "offline" : "online";
    }

    private static String toAppSensorKey(String sensorKey) {
        if (sensorKey == null || sensorKey.isBlank()) {
            return "";
        }

        return switch (sensorKey) {
            case "air_temp" -> "temperature";
            case "air_humidity" -> "humidity";
            case "soil_temp" -> "soilTemp";
            case "soil_moisture" -> "soilHumidity";
            case "soil_ph" -> "soilPh";
            case "light_intensity" -> "light";
            case "co2_ppm" -> "co2";
            case "soil_n" -> "soilN";
            case "soil_p" -> "soilP";
            case "soil_k" -> "soilK";
            default -> sensorKey;
        };
    }

    private static String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
