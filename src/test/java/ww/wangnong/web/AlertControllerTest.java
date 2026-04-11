package ww.wangnong.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.exception.BizException;
import ww.wangnong.service.AlertService;
import ww.wangnong.service.AlertService.AlertListResult;

@WebMvcTest(AlertController.class)
@Import(SecurityConfig.class)
class AlertControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AlertService alertService;

        @Test
        void listAlerts_returnsResult() throws Exception {
                when(alertService.queryAlerts(any()))
                                .thenReturn(new AlertListResult(0, 0, 0, List.of()));

                mockMvc.perform(post("/v1/alerts/list")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"query":{}}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        void handleAlert_returnsOk() throws Exception {
                mockMvc.perform(post("/v1/alerts/handle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"data":{"alert_id":"ALT-001","action":"handle"}}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        void handleAlert_notFound_returns404() throws Exception {
                doThrow(new BizException(404, "告警不存在"))
                                .when(alertService).handleAlert(anyString(), anyString());

                mockMvc.perform(post("/v1/alerts/handle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"data":{"alert_id":"ALT-999","action":"handle"}}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(404))
                                .andExpect(jsonPath("$.message").value("告警不存在"));
        }
}
