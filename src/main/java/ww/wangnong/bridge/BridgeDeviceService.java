package ww.wangnong.bridge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ww.wangnong.repository.CapabilityEquipmentRepository;
import ww.wangnong.repository.CapabilitySensorRepository;
import ww.wangnong.repository.DeviceCapabilityRepository;
import ww.wangnong.repository.DeviceShadowRepository;
import ww.wangnong.service.DatabaseDeviceService;

@Service
@Primary
@ConditionalOnProperty(name = "hmht.run.mode", havingValue = "bridge")
public class BridgeDeviceService extends DatabaseDeviceService {

    public BridgeDeviceService(
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
