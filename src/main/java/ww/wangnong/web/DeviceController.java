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

    @GetMapping("/{deviceId}/capability")
    public ApiResponse<?> getCapability(@PathVariable String deviceId) {
        return ApiResponse.ok(deviceService.getDeviceCapability(deviceId));
    }

    @GetMapping("/{deviceId}/shadow")
    public ApiResponse<?> getShadow(@PathVariable String deviceId) {
        return ApiResponse.ok(deviceService.getDeviceShadow(deviceId));
    }
}
