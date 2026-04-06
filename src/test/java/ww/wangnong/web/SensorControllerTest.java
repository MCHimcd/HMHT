package ww.wangnong.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.service.SensorService;
import ww.wangnong.service.SensorService.EquipmentStatsResult;
import ww.wangnong.service.SensorService.HistoryPoint;
import ww.wangnong.service.SensorService.HistorySummary;
import ww.wangnong.service.SensorService.SensorHistoryResult;
import ww.wangnong.service.SensorService.StatsPoint;

@WebMvcTest(SensorController.class)
@Import(SecurityConfig.class)
class SensorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorService sensorService;

    @Test
    void getSensorHistory_defaultParams_returnsResult() throws Exception {
        when(sensorService.getSensorHistory("N01", "temperature", "24h"))
                .thenReturn(new SensorHistoryResult("temperature",
                        new HistorySummary(30.0, 20.0, 25.0),
                        List.of(new HistoryPoint("08:00", 25.0))));

        mockMvc.perform(get("/v1/sensors/N01/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.metric").value("temperature"));
    }

    @Test
    void getSensorHistory_customMetricAndRange_returnsResult() throws Exception {
        when(sensorService.getSensorHistory("N01", "custom_sensor_x", "7d"))
                .thenReturn(new SensorHistoryResult("custom_sensor_x",
                        new HistorySummary(88.0, 11.0, 49.5),
                        List.of(new HistoryPoint("04-06 10:00", 49.5))));

        mockMvc.perform(get("/v1/sensors/N01/history")
                .param("metric", "custom_sensor_x")
                .param("range", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.metric").value("custom_sensor_x"));
    }

    @Test
    void getEquipmentStats_returnsResult() throws Exception {
        when(sensorService.getEquipmentStats(eq("A03_Node_01"), eq("water_pump")))
                .thenReturn(new EquipmentStatsResult(
                        Map.of("total_value", 100.0, "unit", "灌溉量(升)"),
                        List.of(new StatsPoint("01-01", 10.0))));

        mockMvc.perform(post("/v1/equipment/stats")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"query":{}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
