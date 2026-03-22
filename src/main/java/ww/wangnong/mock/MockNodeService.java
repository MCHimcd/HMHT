package ww.wangnong.mock;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.User;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.service.NodeService;

@Service
public class MockNodeService implements NodeService {

    private final MockAuthService authService;
    private final NodeRepository nodeRepo;
    private final SensorTypeRepository sensorTypeRepo;
    private final NodeSensorRepository nodeSensorRepo;
    private final EquipmentTypeRepository equipmentTypeRepo;
    private final NodeEquipmentRepository nodeEquipmentRepo;

    public MockNodeService(MockAuthService authService,
                           NodeRepository nodeRepo,
                           SensorTypeRepository sensorTypeRepo,
                           NodeSensorRepository nodeSensorRepo,
                           EquipmentTypeRepository equipmentTypeRepo,
                           NodeEquipmentRepository nodeEquipmentRepo) {
        this.authService = authService;
        this.nodeRepo = nodeRepo;
        this.sensorTypeRepo = sensorTypeRepo;
        this.nodeSensorRepo = nodeSensorRepo;
        this.equipmentTypeRepo = equipmentTypeRepo;
        this.nodeEquipmentRepo = nodeEquipmentRepo;
    }

    @Override
    public List<NodeInfo> listNodes(String token) {
        User user = authService.requireUser(token);
        List<Node> nodes = "admin".equals(user.getRole())
                ? nodeRepo.findAll()
                : nodeRepo.findByUserId(user.getId());
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
        if (!"admin".equals(user.getRole()) && !n.getUser().getId().equals(user.getId())) {
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
            if (type == null) return null;
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
            if (type == null) return null;
            return new EquipmentMetaInfo(type.getEquipmentKey(), type.getName(),
                    type.getIcon(), type.getControllable());
        }).filter(java.util.Objects::nonNull).toList();
    }
}
