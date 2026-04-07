package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.entity.CapabilityEquipment;
import ww.wangnong.entity.CapabilitySensor;
import ww.wangnong.entity.DeviceCapability;
import ww.wangnong.entity.DeviceShadow;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.CapabilityEquipmentRepository;
import ww.wangnong.repository.CapabilitySensorRepository;
import ww.wangnong.repository.DeviceCapabilityRepository;
import ww.wangnong.repository.DeviceShadowRepository;
import ww.wangnong.service.DatabaseDeviceService;

@ExtendWith(MockitoExtension.class)
class DatabaseDeviceServiceTest {

    @Mock
    private DeviceCapabilityRepository deviceCapabilityRepository;

    @Mock
    private CapabilitySensorRepository capabilitySensorRepository;

    @Mock
    private CapabilityEquipmentRepository capabilityEquipmentRepository;

    @Mock
    private DeviceShadowRepository deviceShadowRepository;

    private DatabaseDeviceService service;

    @BeforeEach
    void setUp() {
        service = new DatabaseDeviceService(
                deviceCapabilityRepository,
                capabilitySensorRepository,
                capabilityEquipmentRepository,
                deviceShadowRepository);
    }

    @Test
    void getDeviceCapability_existingDevice_returnsAggregatedResult() {
        DeviceCapability capability = new DeviceCapability();
        capability.setDeviceId("D01");
        capability.setCapabilityVersion("2.0.0");
        capability.setChecksum("abc");
        capability.setReportedAt(LocalDateTime.of(2026, 4, 6, 10, 0));

        CapabilitySensor sensor = new CapabilitySensor();
        sensor.setDeviceId("D01");
        sensor.setCapabilityVersion("2.0.0");
        sensor.setSensorKey("temperature");
        sensor.setDataType("double");
        sensor.setUnit("℃");
        sensor.setMinValue(-10.0);
        sensor.setMaxValue(50.0);
        sensor.setRequiredField(true);

        CapabilityEquipment equipment = new CapabilityEquipment();
        equipment.setDeviceId("D01");
        equipment.setCapabilityVersion("2.0.0");
        equipment.setEquipmentKey("water_pump");
        equipment.setControllable(true);
        equipment.setActionsJson("[\"turn_on\",\"turn_off\"]");

        when(deviceCapabilityRepository.findTopByDeviceIdOrderByCreatedAtDesc("D01"))
                .thenReturn(Optional.of(capability));
        when(capabilitySensorRepository.findByDeviceIdAndCapabilityVersionOrderBySensorKey("D01", "2.0.0"))
                .thenReturn(List.of(sensor));
        when(capabilityEquipmentRepository.findByDeviceIdAndCapabilityVersionOrderByEquipmentKey("D01", "2.0.0"))
                .thenReturn(List.of(equipment));

        var result = service.getDeviceCapability("D01");
        assertThat(result.deviceId()).isEqualTo("D01");
        assertThat(result.sensors()).hasSize(1);
        assertThat(result.equipment()).hasSize(1);
        assertThat(result.equipment().getFirst().actions()).containsExactly("turn_on", "turn_off");
    }

    @Test
    void getDeviceCapability_missingDevice_throws404() {
        when(deviceCapabilityRepository.findTopByDeviceIdOrderByCreatedAtDesc("D404"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDeviceCapability("D404"))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(404);
    }

    @Test
    void getDeviceShadow_existingDevice_returnsParsedShadowState() {
        DeviceShadow shadow = new DeviceShadow();
        shadow.setDeviceId("D01");
        shadow.setShadowJson("{\"water_pump\":\"off\",\"last_command_id\":\"CMD-001\"}");
        shadow.setUpdatedAt(LocalDateTime.of(2026, 4, 6, 10, 5));

        when(deviceShadowRepository.findByDeviceId("D01")).thenReturn(Optional.of(shadow));

        var result = service.getDeviceShadow("D01");
        assertThat(result.deviceId()).isEqualTo("D01");
        assertThat(result.shadowState()).containsEntry("last_command_id", "CMD-001");
    }

    @Test
    void getDeviceShadow_missingDevice_throws404() {
        when(deviceShadowRepository.findByDeviceId("D404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDeviceShadow("D404"))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(404);
    }
}
