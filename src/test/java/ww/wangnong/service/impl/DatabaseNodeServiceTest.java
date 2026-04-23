package ww.wangnong.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.entity.EquipmentType;
import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.SensorType;
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
import ww.wangnong.service.NodeService.EquipmentMetaInfo;
import ww.wangnong.service.NodeService.NodeDetail;
import ww.wangnong.service.NodeService.NodeInfo;
import ww.wangnong.service.NodeService.SensorMetaInfo;
import ww.wangnong.service.NodeService.ThresholdUpdate;
import ww.wangnong.service.OperationLogService;

@ExtendWith(MockitoExtension.class)
class DatabaseNodeServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private NodeRepository nodeRepo;
    @Mock
    private SensorTypeRepository sensorTypeRepo;
    @Mock
    private NodeSensorRepository nodeSensorRepo;
    @Mock
    private EquipmentTypeRepository equipmentTypeRepo;
    @Mock
    private NodeEquipmentRepository nodeEquipmentRepo;
    @Mock
    private UserNodeAccessRepository userNodeAccessRepo;
    @Mock
    private OperationLogService opLog;

    private DatabaseNodeService service;

    @BeforeEach
    void setUp() {
        service = new DatabaseNodeService(authService, nodeRepo, sensorTypeRepo,
                nodeSensorRepo, equipmentTypeRepo, nodeEquipmentRepo, userNodeAccessRepo, opLog);
    }

    @Test
    void listNodes_admin_returnsSingleDemoNode() {
        User admin = user(1L, "admin");
        when(authService.requireUser("token")).thenReturn(admin);
        when(nodeRepo.findAll()).thenReturn(List.of(node("SIM_Node_01", "课程模拟温室节点", admin, true)));

        List<NodeInfo> result = service.listNodes("token");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("SIM_Node_01");
        assertThat(result.getFirst().status()).isEqualTo("online");
    }

    @Test
    void listNodes_normalUser_returnsOwnedAndAuthorizedWithoutDuplicates() {
        User user = user(2L, "user");
        when(authService.requireUser("token")).thenReturn(user);
        Node owned = node("SIM_Node_01", "课程模拟温室节点", user, true);
        when(nodeRepo.findByUserId(2L)).thenReturn(List.of(owned));
        UserNodeAccess access = new UserNodeAccess();
        access.setNodeId("SIM_Node_01");
        when(userNodeAccessRepo.findByUserId(2L)).thenReturn(List.of(access));
        when(nodeRepo.findAllByNodeIdIn(List.of("SIM_Node_01"))).thenReturn(List.of(owned));

        List<NodeInfo> result = service.listNodes("token");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("SIM_Node_01");
    }

    @Test
    void getNode_ownerAccess_returnsDetail() {
        User user = user(2L, "user");
        when(authService.requireUser("token")).thenReturn(user);
        when(nodeRepo.findByNodeId("SIM_Node_01"))
                .thenReturn(Optional.of(node("SIM_Node_01", "课程模拟温室节点", user, true)));

        NodeDetail detail = service.getNode("token", "SIM_Node_01");

        assertThat(detail.id()).isEqualTo("SIM_Node_01");
        assertThat(detail.status()).isEqualTo("online");
        assertThat(detail.nodeState()).containsEntry("smartMode", false);
    }

    @Test
    void getNode_noAccess_throwsBizException403() {
        User user = user(2L, "user");
        User owner = user(1L, "user");
        when(authService.requireUser("token")).thenReturn(user);
        when(nodeRepo.findByNodeId("SIM_Node_01"))
                .thenReturn(Optional.of(node("SIM_Node_01", "课程模拟温室节点", owner, true)));
        when(userNodeAccessRepo.existsByUserIdAndNodeId(2L, "SIM_Node_01")).thenReturn(false);

        assertThatThrownBy(() -> service.getNode("token", "SIM_Node_01"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(403);
    }

    @Test
    void getSensorMeta_returnsFixedDictionaryBindings() {
        NodeSensor binding = new NodeSensor();
        binding.setNodeId("SIM_Node_01");
        binding.setSensorKey("temperature");
        binding.setWarningMin(15.0);
        binding.setWarningMax(35.0);
        when(nodeSensorRepo.findByNodeId("SIM_Node_01")).thenReturn(List.of(binding));
        SensorType type = new SensorType();
        type.setSensorKey("temperature");
        type.setName("空气温度");
        type.setUnit("C");
        type.setIcon("temp");
        type.setBadgeColor("#FF7043");
        type.setBgColor("#FFF3E0");
        when(sensorTypeRepo.findBySensorKey("temperature")).thenReturn(Optional.of(type));

        List<SensorMetaInfo> result = service.getSensorMeta("SIM_Node_01");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().key()).isEqualTo("temperature");
        assertThat(result.getFirst().warningMax()).isEqualTo(35.0);
    }

    @Test
    void getEquipmentMeta_returnsFixedDictionaryBindings() {
        NodeEquipment binding = new NodeEquipment();
        binding.setNodeId("SIM_Node_01");
        binding.setEquipmentKey("water_pump");
        when(nodeEquipmentRepo.findByNodeId("SIM_Node_01")).thenReturn(List.of(binding));
        EquipmentType type = new EquipmentType();
        type.setEquipmentKey("water_pump");
        type.setName("水泵");
        type.setIcon("pump");
        type.setControllable(true);
        type.setControlType("toggle");
        when(equipmentTypeRepo.findByEquipmentKey("water_pump")).thenReturn(Optional.of(type));

        List<EquipmentMetaInfo> result = service.getEquipmentMeta("SIM_Node_01");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().key()).isEqualTo("water_pump");
        assertThat(result.getFirst().controlType()).isEqualTo("toggle");
    }

    @Test
    void updateSensorThresholds_authorizedUser_updatesBindingAndLogs() {
        User user = user(2L, "user");
        Node node = node("SIM_Node_01", "课程模拟温室节点", user, true);
        NodeSensor binding = new NodeSensor();
        binding.setNodeId("SIM_Node_01");
        binding.setSensorKey("temperature");
        when(authService.requireUser("token")).thenReturn(user);
        when(nodeRepo.findByNodeId("SIM_Node_01")).thenReturn(Optional.of(node));
        when(nodeSensorRepo.findByNodeIdAndSensorKey("SIM_Node_01", "temperature")).thenReturn(Optional.of(binding));

        service.updateSensorThresholds("token", "SIM_Node_01", List.of(new ThresholdUpdate("temperature", 12.0, 32.0)));

        assertThat(binding.getWarningMin()).isEqualTo(12.0);
        assertThat(binding.getWarningMax()).isEqualTo(32.0);
        verify(nodeSensorRepo).save(binding);
        verify(opLog).log("THRESHOLD_UPDATE", 2L, null, "node", "SIM_Node_01", "1 个传感器阈值已更新");
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Node node(String nodeId, String name, User owner, boolean online) {
        Node node = new Node();
        node.setNodeId(nodeId);
        node.setNodeName(name);
        node.setUser(owner);
        node.setOnline(online);
        node.setSmartMode(false);
        return node;
    }
}
