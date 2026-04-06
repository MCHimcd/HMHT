package ww.wangnong.mock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ww.wangnong.repository.CapabilityEquipmentRepository;
import ww.wangnong.repository.CapabilitySensorRepository;
import ww.wangnong.repository.DeviceCapabilityRepository;
import ww.wangnong.repository.DeviceShadowRepository;
import ww.wangnong.service.DatabaseDeviceService;

@Service
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "mock", matchIfMissing = true)
public class MockDeviceService extends DatabaseDeviceService {

    public MockDeviceService(
            DeviceCapabilityRepository deviceCapabilityRepository,
            CapabilitySensorRepository capabilitySensorRepository,
            CapabilityEquipmentRepository capabilityEquipmentRepository,
            DeviceShadowRepository deviceShadowRepository) {
        super(
                deviceCapabilityRepository,
                capabilitySensorRepository,
                capabilityEquipmentRepository,
                deviceShadowRepository);
    }
}
