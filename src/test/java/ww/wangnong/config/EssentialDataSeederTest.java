package ww.wangnong.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import ww.wangnong.entity.Alert;
import ww.wangnong.entity.EquipmentType;
import ww.wangnong.entity.Node;
import ww.wangnong.entity.NodeEquipment;
import ww.wangnong.entity.NodeSensor;
import ww.wangnong.entity.OperationLog;
import ww.wangnong.entity.ScheduledTask;
import ww.wangnong.entity.SensorType;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;
import ww.wangnong.repository.AlertRepository;
import ww.wangnong.repository.EquipmentTypeRepository;
import ww.wangnong.repository.NodeEquipmentRepository;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.NodeSensorRepository;
import ww.wangnong.repository.OperationLogRepository;
import ww.wangnong.repository.ScheduledTaskRepository;
import ww.wangnong.repository.SensorTypeRepository;
import ww.wangnong.repository.UserNodeAccessRepository;
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
    private UserNodeAccessRepository userNodeAccessRepo;
    @Mock
    private AlertRepository alertRepo;
    @Mock
    private ScheduledTaskRepository taskRepo;
    @Mock
    private OperationLogRepository operationLogRepo;
    @Mock
    private PasswordEncoder passwordEncoder;

    private EssentialDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new EssentialDataSeeder(userRepo, nodeRepo, sensorTypeRepo, equipmentTypeRepo,
                nodeSensorRepo, nodeEquipmentRepo, userNodeAccessRepo, alertRepo, taskRepo,
                operationLogRepo, passwordEncoder);
    }

    @Test
    void run_emptyDatabase_createsCourseSeedData() throws Exception {
        User admin = user(1L, "13800000001", "admin");
        User demoUser = user(2L, "13800000000", "user");
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.empty());
        when(userRepo.findByPhone("13800000000")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("hashed-admin");
        when(passwordEncoder.encode("123456")).thenReturn("hashed-user");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            return "admin".equals(user.getRole()) ? admin : demoUser;
        });
        when(nodeRepo.findByNodeId("SIM_Node_01")).thenReturn(Optional.empty());
        when(sensorTypeRepo.findBySensorKey(anyString())).thenReturn(Optional.empty());
        when(equipmentTypeRepo.findByEquipmentKey(anyString())).thenReturn(Optional.empty());
        when(nodeSensorRepo.findByNodeIdAndSensorKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(nodeEquipmentRepo.findByNodeId("SIM_Node_01")).thenReturn(List.of());
        when(userNodeAccessRepo.existsByUserIdAndNodeId(2L, "SIM_Node_01")).thenReturn(false);
        when(alertRepo.findByAlertId("COURSE_ALERT_1")).thenReturn(Optional.empty());
        when(taskRepo.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
        when(operationLogRepo.count()).thenReturn(0L);

        seeder.run(new DefaultApplicationArguments());

        verify(nodeRepo).save(argThat(node -> "SIM_Node_01".equals(node.getNodeId())));
        verify(sensorTypeRepo, times(4)).save(any(SensorType.class));
        verify(equipmentTypeRepo, times(3)).save(any(EquipmentType.class));
        verify(nodeSensorRepo, times(4)).save(any(NodeSensor.class));
        verify(nodeEquipmentRepo, times(3)).save(any(NodeEquipment.class));
        verify(userNodeAccessRepo).save(any(UserNodeAccess.class));
        verify(alertRepo).save(any(Alert.class));
        verify(taskRepo).save(any(ScheduledTask.class));
        verify(operationLogRepo).save(any(OperationLog.class));
    }

    @Test
    void run_existingUsersWithPlainDefaultPasswords_rehashesPasswords() throws Exception {
        User admin = user(1L, "13800000001", "admin");
        admin.setPasswordHash("admin123");
        User demoUser = user(2L, "13800000000", "user");
        demoUser.setPasswordHash("123456");
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(admin));
        when(userRepo.findByPhone("13800000000")).thenReturn(Optional.of(demoUser));
        when(passwordEncoder.encode("admin123")).thenReturn("rehashed-admin");
        when(passwordEncoder.encode("123456")).thenReturn("rehashed-user");
        stubExistingCourseData(demoUser);

        seeder.run(new DefaultApplicationArguments());

        verify(userRepo).save(argThat(user -> "13800000001".equals(user.getPhone())
                && "rehashed-admin".equals(user.getPasswordHash())));
        verify(userRepo).save(argThat(user -> "13800000000".equals(user.getPhone())
                && "rehashed-user".equals(user.getPasswordHash())));
    }

    private void stubExistingCourseData(User demoUser) {
        lenient().when(nodeRepo.findByNodeId("SIM_Node_01")).thenReturn(Optional.of(new Node()));
        lenient().when(sensorTypeRepo.findBySensorKey(anyString())).thenReturn(Optional.of(new SensorType()));
        lenient().when(equipmentTypeRepo.findByEquipmentKey(anyString())).thenReturn(Optional.of(new EquipmentType()));
        lenient().when(nodeSensorRepo.findByNodeIdAndSensorKey(anyString(), anyString()))
                .thenReturn(Optional.of(new NodeSensor()));
        NodeEquipment equipment = new NodeEquipment();
        equipment.setEquipmentKey("water_pump");
        lenient().when(nodeEquipmentRepo.findByNodeId("SIM_Node_01"))
                .thenReturn(List.of(equipment, equipment, equipment));
        lenient().when(userNodeAccessRepo.existsByUserIdAndNodeId(demoUser.getId(), "SIM_Node_01")).thenReturn(true);
        lenient().when(alertRepo.findByAlertId("COURSE_ALERT_1")).thenReturn(Optional.of(new Alert()));
        lenient().when(taskRepo.findByUserIdOrderByCreatedAtDesc(demoUser.getId()))
                .thenReturn(List.of(new ScheduledTask()));
        lenient().when(operationLogRepo.count()).thenReturn(1L);
    }

    private User user(Long id, String phone, String role) {
        User user = new User();
        user.setId(id);
        user.setPhone(phone);
        user.setRole(role);
        user.setUserName(role);
        user.setPasswordHash("encoded");
        return user;
    }
}
