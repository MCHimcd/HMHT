package ww.wangnong.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.DeviceService;

@RestController
@RequestMapping("/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<?> listDevices() {
        return ApiResponse.ok(deviceService.listDevices());
    }

    @GetMapping("/{deviceId}")
    public ApiResponse<?> getDevice(@PathVariable String deviceId) {
        return ApiResponse.ok(deviceService.getDevice(deviceId));
    }

    @GetMapping("/{deviceId}/sensor-meta")
    public ApiResponse<?> getSensorMeta(@PathVariable String deviceId) {
        return ApiResponse.ok(deviceService.getSensorMeta(deviceId));
    }

    @GetMapping("/{deviceId}/equipment-meta")
    public ApiResponse<?> getEquipmentMeta(@PathVariable String deviceId) {
        return ApiResponse.ok(deviceService.getEquipmentMeta(deviceId));
    }
}
