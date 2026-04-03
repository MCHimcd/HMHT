package ww.wangnong.mock;

import java.util.List;
import java.util.LinkedHashMap;
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
import ww.wangnong.service.NodeService;
import ww.wangnong.service.OperationLogService;

@Service
public class MockNodeService implements NodeService {

    private final MockAuthService authService;
    private final NodeRepository nodeRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final NodeSensorRepository nodeSensorRepo;
    private final EquipmentTypeRepository equipmentTypeRepo;
    private final NodeEquipmentRepository nodeEquipmentRepo;
    private final UserNodeAccessRepository userNodeAccessRepo;
    private final OperationLogService opLog;

    public MockNodeService(MockAuthService authService,
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
                        n.getOnline() ? "online" : "offline"))
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
                n.getOnline() ? "online" : "offline",
                Map.of("smartMode", n.getSmartMode()));
    }

    @Override
    public List<SensorMetaInfo> getSensorMeta(String nodeId) {
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
}
