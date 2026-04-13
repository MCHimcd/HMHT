package ww.wangnong.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.DeviceService;

/**
 * Purpose: 提供设备能力快照与设备影子查询接口。
 * Responsibilities: 将 deviceId 路径参数映射为统一的 DeviceService 查询调用。
 * Flow: GET 请求 -> 读取 deviceId -> DeviceService 查询最新控制面数据 -> ApiResponse.ok。
 * Dependencies: DeviceService、ApiResponse。
 * Input: 设备标识。
 * Output: 设备 capability 或 shadow 快照。
 */
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
