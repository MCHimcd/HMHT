package ww.wangnong.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.repository.UserNodeAccessRepository;
import ww.wangnong.service.AuthService;
import ww.wangnong.service.NodeService;
import ww.wangnong.service.OperationLogService;

/**
 * Purpose: NodeService 的课程版数据库实现。
 * Responsibilities: 查询单个模拟节点、固定传感器/设备元数据和阈值配置。
 * Flow: requireUser -> 按角色和授权过滤节点 -> 从固定字典与绑定表组装节点视图。
 * Dependencies: AuthService、NodeRepository、NodeSensor/EquipmentRepository、固定类型字典仓库。
 * Input: token、nodeId、阈值更新列表。
 * Output: 节点摘要、节点详情、元数据列表或阈值更新副作用。
 */
@Service
public class DatabaseNodeService implements NodeService {

    private final AuthService authService;
    private final NodeRepository nodeRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final NodeSensorRepository nodeSensorRepo;
    private final EquipmentTypeRepository equipmentTypeRepo;
    private final NodeEquipmentRepository nodeEquipmentRepo;
    private final UserNodeAccessRepository userNodeAccessRepo;
    private final OperationLogService opLog;

    public DatabaseNodeService(AuthService authService,
            NodeRepository nodeRepo,
            SensorTypeRepository sensorTypeRepo,
            NodeSensorRepository nodeSensorRepo,
            EquipmentTypeRepository equipmentTypeRepo,
            NodeEquipmentRepository nodeEquipmentRepo,
            UserNodeAccessRepository userNodeAccessRepo,
            OperationLogService opLog) {
        this.authService = authService;
        this.nodeRepo = nodeRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.nodeSensorRepo = nodeSensorRepo;
        this.equipmentTypeRepo = equipmentTypeRepo;
        this.nodeEquipmentRepo = nodeEquipmentRepo;
        this.userNodeAccessRepo = userNodeAccessRepo;
        this.opLog = opLog;
    }

    @Override
    public List<NodeInfo> listNodes(String token) {
        User user = authService.requireUser(token);
        List<Node> nodes;
        if ("admin".equals(user.getRole())) {
            nodes = nodeRepo.findAll();
        } else {
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
                .map(node -> new NodeInfo(node.getNodeId(), node.getNodeName(), resolveNodeStatus(node)))
                .toList();
    }

    @Override
    public NodeDetail getNode(String token, String nodeId) {
        User user = authService.requireUser(token);
        Node node = nodeRepo.findByNodeId(nodeId)
                .orElseThrow(() -> new BizException(404, "节点不存在"));
        if (!canAccess(user, node)) {
            throw new BizException(403, "无权访问该节点");
        }
        return new NodeDetail(node.getNodeId(), node.getNodeName(),
                resolveNodeStatus(node), Map.of("smartMode", node.getSmartMode()));
    }

    @Override
    public List<SensorMetaInfo> getSensorMeta(String nodeId) {
        return nodeSensorRepo.findByNodeId(nodeId).stream().map(nodeSensor -> {
            var type = sensorTypeRepo.findBySensorKey(nodeSensor.getSensorKey()).orElse(null);
            if (type == null) {
                return null;
            }
            return new SensorMetaInfo(type.getSensorKey(), type.getName(), type.getUnit(),
                    type.getIcon(), type.getBadgeColor(), type.getBgColor(),
                    nodeSensor.getWarningMin(), nodeSensor.getWarningMax());
        }).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public List<EquipmentMetaInfo> getEquipmentMeta(String nodeId) {
        return nodeEquipmentRepo.findByNodeId(nodeId).stream().map(nodeEquipment -> {
            var type = equipmentTypeRepo.findByEquipmentKey(nodeEquipment.getEquipmentKey()).orElse(null);
            if (type == null) {
                return null;
            }
            return new EquipmentMetaInfo(type.getEquipmentKey(), type.getName(), type.getIcon(),
                    Boolean.TRUE.equals(type.getControllable()),
                    type.getControlType() == null ? "toggle" : type.getControlType());
        }).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public void updateSensorThresholds(String token, String nodeId, List<ThresholdUpdate> thresholds) {
        User user = authService.requireUser(token);
        Node node = nodeRepo.findByNodeId(nodeId)
                .orElseThrow(() -> new BizException(404, "节点不存在"));
        if (!canAccess(user, node)) {
            throw new BizException(403, "无权访问该节点");
        }
        for (ThresholdUpdate update : thresholds) {
            NodeSensor nodeSensor = nodeSensorRepo.findByNodeIdAndSensorKey(nodeId, update.sensorKey())
                    .orElseThrow(() -> new BizException(404, "传感器不存在: " + update.sensorKey()));
            nodeSensor.setWarningMin(update.warningMin());
            nodeSensor.setWarningMax(update.warningMax());
            nodeSensorRepo.save(nodeSensor);
        }
        opLog.log("THRESHOLD_UPDATE", user.getId(), user.getUserName(),
                "node", nodeId, thresholds.size() + " 个传感器阈值已更新");
    }

    private boolean canAccess(User user, Node node) {
        return "admin".equals(user.getRole())
                || (node.getUser() != null && node.getUser().getId().equals(user.getId()))
                || userNodeAccessRepo.existsByUserIdAndNodeId(user.getId(), node.getNodeId());
    }

    private String resolveNodeStatus(Node node) {
        return Boolean.TRUE.equals(node.getOnline()) ? "online" : "offline";
    }
}
