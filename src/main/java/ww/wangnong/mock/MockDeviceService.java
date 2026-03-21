package ww.wangnong.mock;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ww.wangnong.entity.Device;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.DeviceRepository;
import ww.wangnong.repository.EquipmentMetaRepository;
import ww.wangnong.repository.SensorMetaRepository;
import ww.wangnong.service.DeviceService;

@Service
public class MockDeviceService implements DeviceService {

    private final DeviceRepository deviceRepo;
    private final SensorMetaRepository sensorMetaRepo;
    private final EquipmentMetaRepository equipmentMetaRepo;

    public MockDeviceService(DeviceRepository deviceRepo,
                             SensorMetaRepository sensorMetaRepo,
                             EquipmentMetaRepository equipmentMetaRepo) {
        this.deviceRepo = deviceRepo;
        this.sensorMetaRepo = sensorMetaRepo;
        this.equipmentMetaRepo = equipmentMetaRepo;
    }

    @Override
    public List<DeviceInfo> listDevices() {
        return deviceRepo.findAll().stream()
                .map(d -> new DeviceInfo(d.getDeviceId(), d.getDeviceName(),
                        d.getOnline() ? "online" : "offline"))
                .toList();
    }

    @Override
    public DeviceDetail getDevice(String deviceId) {
        Device d = deviceRepo.findByDeviceId(deviceId)
                .orElseThrow(() -> new BizException(404, "设备不存在"));
        return new DeviceDetail(d.getDeviceId(), d.getDeviceName(),
                d.getOnline() ? "online" : "offline",
                Map.of("smartMode", d.getSmartMode()));
    }

    @Override
    public List<SensorMetaInfo> getSensorMeta(String deviceId) {
        return sensorMetaRepo.findByDeviceId(deviceId).stream()
                .map(s -> new SensorMetaInfo(s.getSensorKey(), s.getName(), s.getUnit(),
                        s.getIcon(), s.getBadgeColor(), s.getBgColor(),
                        s.getWarningMin(), s.getWarningMax()))
                .toList();
    }

    @Override
    public List<EquipmentMetaInfo> getEquipmentMeta(String deviceId) {
        return equipmentMetaRepo.findByDeviceId(deviceId).stream()
                .map(e -> new EquipmentMetaInfo(e.getEquipmentKey(), e.getName(),
                        e.getIcon(), e.getControllable()))
                .toList();
    }
}
