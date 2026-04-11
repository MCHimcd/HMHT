package ww.wangnong.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ww.wangnong.service.AlertService;

class AlertE2eTest extends E2eTestBase {

    @Autowired
    private AlertService alertService;

    @Test
    void queryAndHandleAlert_fullFlow() throws Exception {
        // 1) Seed an alert directly
        alertService.addAlert(new AlertService.AlertInfo(
                "E2E-ALT-001", "SIM_Node_01", "temperature_alert", "温度异常",
                "warning", "温度超出阈值 35℃", "temperature", "空气温度",
                false, null, System.currentTimeMillis()));

        // 2) Query alerts — our alert is the newest, appears first (MockDataLifecycle
        // seeds others)
        mockMvc.perform(post("/v1/alerts/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"query":{}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].alertId").value("E2E-ALT-001"));

        // 3) Handle the alert
        mockMvc.perform(post("/v1/alerts/handle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"data":{"alert_id":"E2E-ALT-001","action":"handle"}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 4) Query with filter_status=handled — should include our handled alert
        mockMvc.perform(post("/v1/alerts/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"query":{"filter_status":"handled"}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].isHandled").value(true));
    }
}
