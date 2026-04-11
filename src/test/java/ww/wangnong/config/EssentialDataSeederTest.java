package ww.wangnong.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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

        when(userRepo.existsByPhone("13800000001")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hashed");
        when(sensorTypeRepo.count()).thenReturn(0L);
        when(equipmentTypeRepo.count()).thenReturn(0L);

        seeder.run(new DefaultApplicationArguments());

        verify(userRepo).save(any(User.class));
        verify(sensorTypeRepo, times(6)).save(any(SensorType.class));
        verify(equipmentTypeRepo, times(4)).save(any(EquipmentType.class));
        verify(nodeSensorRepo, times(6)).save(any(NodeSensor.class));
        verify(nodeEquipmentRepo, times(4)).save(any(NodeEquipment.class));
    }

    @Test
    void run_existingData_skipsCreation() throws Exception {
        RunModeHolder holder = new RunModeHolder("mock");
        EssentialDataSeeder seeder = new EssentialDataSeeder(
                userRepo, nodeRepo, sensorTypeRepo, equipmentTypeRepo,
                nodeSensorRepo, nodeEquipmentRepo, holder, passwordEncoder);

        when(userRepo.existsByPhone("13800000001")).thenReturn(true);
        when(nodeRepo.findByNodeId(any())).thenReturn(java.util.Optional.of(new Node()));
        when(sensorTypeRepo.count()).thenReturn(6L);
        when(equipmentTypeRepo.count()).thenReturn(4L);
        when(nodeSensorRepo.findByNodeId("SIM_Node_01")).thenReturn(List.of(new NodeSensor()));
        when(nodeEquipmentRepo.findByNodeId("SIM_Node_01")).thenReturn(List.of(new NodeEquipment()));

        seeder.run(new DefaultApplicationArguments());

        verify(userRepo, never()).save(any(User.class));
        verify(sensorTypeRepo, never()).save(any(SensorType.class));
    }

    @Test
    void run_hardwareMode_skipsSimNodeSensorsAndEquipment() throws Exception {
        RunModeHolder holder = new RunModeHolder("hardware");
        EssentialDataSeeder seeder = new EssentialDataSeeder(
                userRepo, nodeRepo, sensorTypeRepo, equipmentTypeRepo,
                nodeSensorRepo, nodeEquipmentRepo, holder, passwordEncoder);

        when(userRepo.existsByPhone("13800000001")).thenReturn(true);
        when(nodeRepo.findByNodeId(any())).thenReturn(java.util.Optional.of(new Node()));
        when(sensorTypeRepo.count()).thenReturn(6L);
        when(equipmentTypeRepo.count()).thenReturn(4L);

        seeder.run(new DefaultApplicationArguments());

        verify(nodeSensorRepo, never()).save(any(NodeSensor.class));
        verify(nodeEquipmentRepo, never()).save(any(NodeEquipment.class));
    }
}
