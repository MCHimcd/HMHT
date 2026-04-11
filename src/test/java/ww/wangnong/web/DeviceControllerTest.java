package ww.wangnong.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.exception.BizException;
import ww.wangnong.service.DeviceService;
import ww.wangnong.service.DeviceService.DeviceCapabilityResult;
import ww.wangnong.service.DeviceService.DeviceShadowResult;
import ww.wangnong.service.DeviceService.EquipmentCapability;
import ww.wangnong.service.DeviceService.SensorCapability;

@WebMvcTest(DeviceController.class)
@Import(SecurityConfig.class)
class DeviceControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DeviceService deviceService;

        @Test
        void getCapability_existingDevice_returnsCapability() throws Exception {
                when(deviceService.getDeviceCapability("D01")).thenReturn(new DeviceCapabilityResult(
                                "D01",
                                "2.0.0",
                                "sha256:abc",
                                LocalDateTime.of(2026, 4, 6, 10, 0),
                                List.of(new SensorCapability("temperature", "double", "℃", -10.0, 50.0, true)),
                                List.of(new EquipmentCapability("water_pump", true, List.of("turn_on", "turn_off")))));

                mockMvc.perform(get("/v1/devices/D01/capability"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.deviceId").value("D01"))
                                .andExpect(jsonPath("$.data.sensors[0].sensorKey").value("temperature"));
        }

        @Test
        void getShadow_existingDevice_returnsShadow() throws Exception {
                when(deviceService.getDeviceShadow("D01")).thenReturn(new DeviceShadowResult(
                                "D01",
                                Map.of("water_pump", "off", "last_command_id", "CMD-1"),
                                LocalDateTime.of(2026, 4, 6, 10, 5)));

                mockMvc.perform(get("/v1/devices/D01/shadow"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.deviceId").value("D01"))
                                .andExpect(jsonPath("$.data.shadowState.last_command_id").value("CMD-1"));
        }

        @Test
        void getCapability_notFound_returns404() throws Exception {
                when(deviceService.getDeviceCapability("D99"))
                                .thenThrow(new BizException(404, "设备不存在"));

                mockMvc.perform(get("/v1/devices/D99/capability"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(404))
                                .andExpect(jsonPath("$.message").value("设备不存在"));
        }

        @Test
        void getShadow_notFound_returns404() throws Exception {
                when(deviceService.getDeviceShadow("D99"))
                                .thenThrow(new BizException(404, "设备不存在"));

                mockMvc.perform(get("/v1/devices/D99/shadow"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(404))
                                .andExpect(jsonPath("$.message").value("设备不存在"));
        }
}
