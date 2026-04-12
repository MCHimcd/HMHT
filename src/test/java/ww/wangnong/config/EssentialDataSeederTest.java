package ww.wangnong.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import ww.wangnong.entity.EquipmentType;
import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.SensorType;
import ww.wangnong.entity.User;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EssentialDataSeederTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private NodeRepository nodeRepo;
    @Mock
    private SensorTypeRepository sensorTypeRepo;
    @Mock
    private EquipmentTypeRepository equipmentTypeRepo;
    @Mock
    private NodeSensorRepository nodeSensorRepo;
    @Mock
    private NodeEquipmentRepository nodeEquipmentRepo;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void run_emptyDatabase_createsAdminAndSeedData() throws Exception {
        RunModeHolder holder = new RunModeHolder("mock");
        EssentialDataSeeder seeder = new EssentialDataSeeder(
                userRepo, nodeRepo, sensorTypeRepo, equipmentTypeRepo,
                nodeSensorRepo, nodeEquipmentRepo, holder, passwordEncoder);

        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("hashed");
        when(sensorTypeRepo.count()).thenReturn(0L);
        when(equipmentTypeRepo.findByEquipmentKey(anyString())).thenReturn(Optional.empty());

        seeder.run(new DefaultApplicationArguments());

        verify(userRepo).save(any(User.class));
        verify(sensorTypeRepo, times(6)).save(any(SensorType.class));
        verify(equipmentTypeRepo, times(5)).save(any(EquipmentType.class));
        verify(nodeSensorRepo, times(6)).save(any(NodeSensor.class));
        verify(nodeEquipmentRepo, times(15)).save(any(NodeEquipment.class));
    }

    @Test
    void run_existingData_skipsCreation() throws Exception {
        RunModeHolder holder = new RunModeHolder("mock");
        EssentialDataSeeder seeder = new EssentialDataSeeder(
                userRepo, nodeRepo, sensorTypeRepo, equipmentTypeRepo,
                nodeSensorRepo, nodeEquipmentRepo, holder, passwordEncoder);

        User admin = new User();
        admin.setPhone("13800000001");
        admin.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv123456789012345678901234567890");
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(admin));
        when(nodeRepo.findByNodeId(any())).thenReturn(java.util.Optional.of(new Node()));
        when(sensorTypeRepo.count()).thenReturn(6L);
        when(equipmentTypeRepo.findByEquipmentKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            return Optional.of(existingEquipmentType(key));
        });
        when(nodeSensorRepo.findByNodeId("SIM_Node_01")).thenReturn(List.of(new NodeSensor()));
        when(nodeEquipmentRepo.findByNodeId(anyString())).thenAnswer(invocation -> {
            List<NodeEquipment> result = new java.util.ArrayList<>();
            for (String k : List.of("water_valve", "water_pump", "led_light", "fan", "camera")) {
                NodeEquipment ne = new NodeEquipment();
                ne.setEquipmentKey(k);
                result.add(ne);
            }
            return result;
        });

        seeder.run(new DefaultApplicationArguments());

        verify(userRepo, never()).save(any(User.class));
        verify(sensorTypeRepo, never()).save(any(SensorType.class));
        verify(nodeEquipmentRepo, never()).save(any(NodeEquipment.class));
    }

    @Test
    void run_hardwareMode_stillCreatesSimNodeSensorsAndEquipment() throws Exception {
        RunModeHolder holder = new RunModeHolder("hardware");
        EssentialDataSeeder seeder = new EssentialDataSeeder(
                userRepo, nodeRepo, sensorTypeRepo, equipmentTypeRepo,
                nodeSensorRepo, nodeEquipmentRepo, holder, passwordEncoder);

        User admin = new User();
        admin.setPhone("13800000001");
        admin.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv123456789012345678901234567890");
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(admin));
        when(nodeRepo.findByNodeId(any())).thenReturn(java.util.Optional.of(new Node()));
        when(sensorTypeRepo.count()).thenReturn(6L);
        when(equipmentTypeRepo.findByEquipmentKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            return Optional.of(existingEquipmentType(key));
        });

        seeder.run(new DefaultApplicationArguments());

        verify(nodeSensorRepo, times(6)).save(any(NodeSensor.class));
        verify(nodeEquipmentRepo, times(15)).save(any(NodeEquipment.class));
    }

    @Test
    void run_existingLegacyAdminPassword_upgradesToEncodedHash() throws Exception {
        RunModeHolder holder = new RunModeHolder("mock");
        EssentialDataSeeder seeder = new EssentialDataSeeder(
                userRepo, nodeRepo, sensorTypeRepo, equipmentTypeRepo,
                nodeSensorRepo, nodeEquipmentRepo, holder, passwordEncoder);

        User admin = new User();
        admin.setId(1L);
        admin.setPhone("13800000001");
        admin.setUserName("管理员");
        admin.setRole("admin");
        admin.setGender("男");
        admin.setBirthDate(LocalDate.of(1985, 6, 15));
        admin.setPasswordHash("admin123");
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("admin123")).thenReturn("rehashed-admin-password");
        when(nodeRepo.findByNodeId(any())).thenReturn(java.util.Optional.of(new Node()));
        when(sensorTypeRepo.count()).thenReturn(6L);
        when(equipmentTypeRepo.findByEquipmentKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            return Optional.of(existingEquipmentType(key));
        });
        when(nodeSensorRepo.findByNodeId("SIM_Node_01")).thenReturn(List.of(new NodeSensor()));
        when(nodeEquipmentRepo.findByNodeId("SIM_Node_01")).thenReturn(List.of(new NodeEquipment()));

        seeder.run(new DefaultApplicationArguments());

        verify(userRepo).save(argThat(user -> "13800000001".equals(user.getPhone())
                && "rehashed-admin-password".equals(user.getPasswordHash())));
    }

    private EquipmentType existingEquipmentType(String key) {
        EquipmentType type = new EquipmentType();
        type.setEquipmentKey(key);
        switch (key) {
            case "water_valve":
                type.setName("水阀");
                type.setIcon("🚿");
                type.setControllable(true);
                type.setControlType("toggle");
                break;
            case "water_pump":
                type.setName("水泵");
                type.setIcon("💦");
                type.setControllable(true);
                type.setControlType("toggle");
                break;
            case "led_light":
                type.setName("补光灯");
                type.setIcon("💡");
                type.setControllable(true);
                type.setControlType("slider");
                break;
            case "camera":
                type.setName("摄像头");
                type.setIcon("📷");
                type.setControllable(false);
                type.setControlType("toggle");
                break;
            case "fan":
                type.setName("排风扇");
                type.setIcon("🌀");
                type.setControllable(true);
                type.setControlType("toggle");
                break;
            default:
                break;
        }
        return type;
    }
}
