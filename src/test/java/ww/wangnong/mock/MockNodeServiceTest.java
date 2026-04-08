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
import ww.wangnong.entity.CapabilitySensor;
import ww.wangnong.entity.CapabilityEquipment;
import ww.wangnong.entity.DeviceCapability;
import ww.wangnong.entity.DeviceProfile;
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
    private DeviceCapabilityRepository deviceCapabilityRepository;
    @Mock
    private CapabilitySensorRepository capabilitySensorRepository;
    @Mock
    private CapabilityEquipmentRepository capabilityEquipmentRepository;
    @Mock
    private DeviceProfileRepository deviceProfileRepository;
    @Mock
    private OperationLogService opLog;

    private MockNodeService service;

    @BeforeEach
    void setUp() {
        service = new MockNodeService(authService, nodeRepo, sensorTypeRepo,
                nodeSensorRepo, equipmentTypeRepo, nodeEquipmentRepo,
                userNodeAccessRepo,
                deviceCapabilityRepository,
                capabilitySensorRepository,
                capabilityEquipmentRepository,
                deviceProfileRepository,
                opLog);
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

    private DeviceCapability stubCapability(String deviceId, String version) {
        DeviceCapability capability = new DeviceCapability();
        capability.setDeviceId(deviceId);
        capability.setCapabilityVersion(version);
        return capability;
    }

    // ── listNodes ──

    @Test
    void listNodes_admin_returnsAllNodes() {
        User admin = stubUser(1L, "admin");
        when(authService.requireUser("token")).thenReturn(admin);

        Node n1 = stubNode("N01", "温室1", admin);
        Node n2 = stubNode("N02", "温室2", admin);
        when(nodeRepo.findAll()).thenReturn(List.of(n1, n2));
        when(deviceProfileRepository.findByDeviceId(anyString())).thenReturn(Optional.empty());

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
        when(deviceProfileRepository.findByDeviceId(anyString())).thenReturn(Optional.empty());

        List<NodeInfo> result = service.listNodes("token");

        assertThat(result).hasSize(2);
    }

    @Test
    void listNodes_normalUser_overlapOwnedAndAuthorized_deduplicatesByNodeId() {
        User user = stubUser(2L, "user");
        when(authService.requireUser("token")).thenReturn(user);

        Node owned = stubNode("N01", "我的", user);
        when(nodeRepo.findByUserId(2L)).thenReturn(List.of(owned));

        UserNodeAccess access = new UserNodeAccess();
        access.setNodeId("N01");
        when(userNodeAccessRepo.findByUserId(2L)).thenReturn(List.of(access));

        User other = stubUser(1L, "admin");
        Node accessedDuplicate = stubNode("N01", "授权重复", other);
        when(nodeRepo.findAllByNodeIdIn(List.of("N01"))).thenReturn(List.of(accessedDuplicate));
        when(deviceProfileRepository.findByDeviceId(anyString())).thenReturn(Optional.empty());

        List<NodeInfo> result = service.listNodes("token");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("N01");
    }

    // ── getNode ──

    @Test
    void getNode_ownerAccess_returnsDetail() {
        User user = stubUser(1L, "user");
        when(authService.requireUser("token")).thenReturn(user);

        Node node = stubNode("N01", "温室1", user);
        when(nodeRepo.findByNodeId("N01")).thenReturn(Optional.of(node));
        when(deviceProfileRepository.findByDeviceId("N01")).thenReturn(Optional.empty());

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

    @Test
    void listNodes_profileOnlineButLastSeenExpired_returnsOffline() {
        User admin = stubUser(1L, "admin");
        when(authService.requireUser("token")).thenReturn(admin);

        Node n1 = stubNode("N01", "温室1", admin);
        n1.setOnline(true);
        when(nodeRepo.findAll()).thenReturn(List.of(n1));

        DeviceProfile profile = new DeviceProfile();
        profile.setDeviceId("N01");
        profile.setOnline(true);
        profile.setLastSeenAt(java.time.LocalDateTime.now().minusMinutes(30));
        when(deviceProfileRepository.findByDeviceId("N01")).thenReturn(Optional.of(profile));

        List<NodeInfo> result = service.listNodes("token");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo("offline");
    }

    @Test
    void getNode_profileOnlineAndRecentLastSeen_returnsOnline() {
        User user = stubUser(1L, "user");
        when(authService.requireUser("token")).thenReturn(user);

        Node node = stubNode("N01", "温室1", user);
        node.setOnline(false);
        when(nodeRepo.findByNodeId("N01")).thenReturn(Optional.of(node));

        DeviceProfile profile = new DeviceProfile();
        profile.setDeviceId("N01");
        profile.setOnline(true);
        profile.setLastSeenAt(java.time.LocalDateTime.now());
        when(deviceProfileRepository.findByDeviceId("N01")).thenReturn(Optional.of(profile));

        NodeDetail detail = service.getNode("token", "N01");

        assertThat(detail.status()).isEqualTo("online");
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
        when(deviceCapabilityRepository.findTopByDeviceIdOrderByCreatedAtDesc("N01")).thenReturn(Optional.empty());

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
        when(deviceCapabilityRepository.findTopByDeviceIdOrderByCreatedAtDesc("N01")).thenReturn(Optional.empty());

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

    @Test
    void getSensorMeta_withCapability_prefersCapabilityAndAllowsUnknown() {
        DeviceCapability capability = stubCapability("N01", "2.0");
        when(deviceCapabilityRepository.findTopByDeviceIdOrderByCreatedAtDesc("N01"))
                .thenReturn(Optional.of(capability));

        CapabilitySensor airTemp = new CapabilitySensor();
        airTemp.setDeviceId("N01");
        airTemp.setCapabilityVersion("2.0");
        airTemp.setSensorKey("air_temp");
        airTemp.setDataType("number");

        CapabilitySensor custom = new CapabilitySensor();
        custom.setDeviceId("N01");
        custom.setCapabilityVersion("2.0");
        custom.setSensorKey("custom_sensor_x");
        custom.setDataType("number");

        when(capabilitySensorRepository.findByDeviceIdAndCapabilityVersionOrderBySensorKey("N01", "2.0"))
                .thenReturn(List.of(airTemp, custom));

        NodeSensor threshold = new NodeSensor();
        threshold.setNodeId("N01");
        threshold.setSensorKey("temperature");
        threshold.setWarningMin(10.0);
        threshold.setWarningMax(35.0);
        when(nodeSensorRepo.findByNodeId("N01")).thenReturn(List.of(threshold));

        SensorType sensorType = new SensorType();
        sensorType.setSensorKey("temperature");
        sensorType.setName("空气温度");
        sensorType.setUnit("℃");
        sensorType.setIcon("🌡️");
        sensorType.setBadgeColor("#FF7043");
        sensorType.setBgColor("#FFF3E0");
        when(sensorTypeRepo.findBySensorKey("temperature")).thenReturn(Optional.of(sensorType));
        when(sensorTypeRepo.findBySensorKey("custom_sensor_x")).thenReturn(Optional.empty());

        List<SensorMetaInfo> result = service.getSensorMeta("N01");

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(SensorMetaInfo::key)).containsExactly("temperature", "custom_sensor_x");
        assertThat(result.stream().map(SensorMetaInfo::key)).doesNotContain("humidity");

        SensorMetaInfo mapped = result.getFirst();
        assertThat(mapped.key()).isEqualTo("temperature");
        assertThat(mapped.warningMin()).isEqualTo(10.0);
        assertThat(mapped.warningMax()).isEqualTo(35.0);

        SensorMetaInfo unknown = result.get(1);
        assertThat(unknown.key()).isEqualTo("custom_sensor_x");
        assertThat(unknown.name()).isEqualTo("custom_sensor_x");
    }

    @Test
    void getEquipmentMeta_withCapability_prefersCapabilityAndAllowsUnknown() {
        DeviceCapability capability = stubCapability("N01", "2.0");
        when(deviceCapabilityRepository.findTopByDeviceIdOrderByCreatedAtDesc("N01"))
                .thenReturn(Optional.of(capability));

        CapabilityEquipment waterPump = new CapabilityEquipment();
        waterPump.setDeviceId("N01");
        waterPump.setCapabilityVersion("2.0");
        waterPump.setEquipmentKey("water_pump");
        waterPump.setControllable(true);

        CapabilityEquipment custom = new CapabilityEquipment();
        custom.setDeviceId("N01");
        custom.setCapabilityVersion("2.0");
        custom.setEquipmentKey("custom_device_y");
        custom.setControllable(false);

        when(capabilityEquipmentRepository.findByDeviceIdAndCapabilityVersionOrderByEquipmentKey("N01", "2.0"))
                .thenReturn(List.of(custom, waterPump));

        EquipmentType equipmentType = new EquipmentType();
        equipmentType.setEquipmentKey("water_pump");
        equipmentType.setName("水泵");
        equipmentType.setIcon("💦");
        equipmentType.setControllable(true);
        when(equipmentTypeRepo.findByEquipmentKey("water_pump")).thenReturn(Optional.of(equipmentType));
        when(equipmentTypeRepo.findByEquipmentKey("custom_device_y")).thenReturn(Optional.empty());

        List<EquipmentMetaInfo> result = service.getEquipmentMeta("N01");

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(EquipmentMetaInfo::key))
                .containsExactly("custom_device_y", "water_pump");
        assertThat(result.stream().map(EquipmentMetaInfo::key)).doesNotContain("fan");

        EquipmentMetaInfo unknown = result.getFirst();
        assertThat(unknown.key()).isEqualTo("custom_device_y");
        assertThat(unknown.name()).isEqualTo("custom_device_y");
        assertThat(unknown.controllable()).isFalse();
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
