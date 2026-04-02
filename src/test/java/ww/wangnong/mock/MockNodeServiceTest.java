package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import ww.wangnong.service.NodeService.EquipmentMetaInfo;
import ww.wangnong.service.NodeService.NodeDetail;
import ww.wangnong.service.NodeService.NodeInfo;
import ww.wangnong.service.NodeService.SensorMetaInfo;
import ww.wangnong.service.OperationLogService;

@ExtendWith(MockitoExtension.class)
class MockNodeServiceTest {

    @Mock
    private MockAuthService authService;
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

    private MockNodeService service;

    @BeforeEach
    void setUp() {
        service = new MockNodeService(authService, nodeRepo, sensorTypeRepo,
                nodeSensorRepo, equipmentTypeRepo, nodeEquipmentRepo,
                userNodeAccessRepo, opLog);
    }

    private User stubUser(Long id, String role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    private Node stubNode(String nodeId, String name, User owner) {
        Node n = new Node();
        n.setNodeId(nodeId);
        n.setNodeName(name);
        n.setUser(owner);
        n.setOnline(true);
        n.setSmartMode(false);
        return n;
    }

    // ── listNodes ──

    @Test
    void listNodes_admin_returnsAllNodes() {
        User admin = stubUser(1L, "admin");
        when(authService.requireUser("token")).thenReturn(admin);

        Node n1 = stubNode("N01", "温室1", admin);
        Node n2 = stubNode("N02", "温室2", admin);
        when(nodeRepo.findAll()).thenReturn(List.of(n1, n2));

        List<NodeInfo> result = service.listNodes("token");

        assertThat(result).hasSize(2);
    }

    @Test
    void listNodes_normalUser_returnsOwnedAndAuthorized() {
        User user = stubUser(2L, "user");
        when(authService.requireUser("token")).thenReturn(user);

        Node owned = stubNode("N01", "我的", user);
        when(nodeRepo.findByUserId(2L)).thenReturn(List.of(owned));

        UserNodeAccess access = new UserNodeAccess();
        access.setNodeId("N02");
        when(userNodeAccessRepo.findByUserId(2L)).thenReturn(List.of(access));

        User other = stubUser(1L, "admin");
        Node accessed = stubNode("N02", "授权的", other);
        when(nodeRepo.findAllByNodeIdIn(List.of("N02"))).thenReturn(List.of(accessed));

        List<NodeInfo> result = service.listNodes("token");

        assertThat(result).hasSize(2);
    }

    // ── getNode ──

    @Test
    void getNode_ownerAccess_returnsDetail() {
        User user = stubUser(1L, "user");
        when(authService.requireUser("token")).thenReturn(user);

        Node node = stubNode("N01", "温室1", user);
        when(nodeRepo.findByNodeId("N01")).thenReturn(Optional.of(node));

        NodeDetail detail = service.getNode("token", "N01");

        assertThat(detail.id()).isEqualTo("N01");
        assertThat(detail.status()).isEqualTo("online");
    }

    @Test
    void getNode_noAccess_throwsBizException403() {
        User user = stubUser(2L, "user");
        when(authService.requireUser("token")).thenReturn(user);

        User owner = stubUser(1L, "user");
        Node node = stubNode("N01", "温室1", owner);
        when(nodeRepo.findByNodeId("N01")).thenReturn(Optional.of(node));
        when(userNodeAccessRepo.existsByUserIdAndNodeId(2L, "N01")).thenReturn(false);

        assertThatThrownBy(() -> service.getNode("token", "N01"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(403);
    }

    @Test
    void getNode_nonExistent_throwsBizException404() {
        User user = stubUser(1L, "user");
        when(authService.requireUser("token")).thenReturn(user);
        when(nodeRepo.findByNodeId("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNode("token", "NONEXISTENT"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(404);
    }

    // ── getSensorMeta ──

    @Test
    void getSensorMeta_returnsMappedInfo() {
        NodeSensor ns = new NodeSensor();
        ns.setNodeId("N01");
        ns.setSensorKey("temperature");
        ns.setWarningMin(10.0);
        ns.setWarningMax(35.0);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(ns));

        SensorType st = new SensorType();
        st.setSensorKey("temperature");
        st.setName("温度");
        st.setUnit("℃");
        when(sensorTypeRepo.findBySensorKey("temperature")).thenReturn(Optional.of(st));

        List<SensorMetaInfo> result = service.getSensorMeta("N01");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().key()).isEqualTo("temperature");
        assertThat(result.getFirst().warningMin()).isEqualTo(10.0);
    }

    // ── getEquipmentMeta ──

    @Test
    void getEquipmentMeta_returnsMappedInfo() {
        NodeEquipment ne = new NodeEquipment();
        ne.setNodeId("N01");
        ne.setEquipmentKey("water_pump");
        when(nodeEquipmentRepo.findByNodeId("N01")).thenReturn(List.of(ne));

        EquipmentType et = new EquipmentType();
        et.setEquipmentKey("water_pump");
        et.setName("水泵");
        et.setControllable(true);
        when(equipmentTypeRepo.findByEquipmentKey("water_pump")).thenReturn(Optional.of(et));

        List<EquipmentMetaInfo> result = service.getEquipmentMeta("N01");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().key()).isEqualTo("water_pump");
        assertThat(result.getFirst().controllable()).isTrue();
    }

    // ── updateSensorThresholds ──

    @Test
    void updateSensorThresholds_nonExistentNode_throwsBizException404() {
        User user = stubUser(1L, "user");
        when(authService.requireUser("token")).thenReturn(user);
        when(nodeRepo.findByNodeId("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSensorThresholds("token", "NONEXISTENT", List.of()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(404);
    }
}
