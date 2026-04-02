package ww.wangnong.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.mqtt.PahoMqttService;

@WebMvcTest(InfraController.class)
@Import(SecurityConfig.class)
class InfraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PahoMqttService mqttService;

    @Test
    void health_returnsStatusInfo() throws Exception {
        when(mqttService.isConnected()).thenReturn(false);

        mockMvc.perform(get("/infra/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").exists())
                .andExpect(jsonPath("$.mqttConnected").value(false));
    }

    @Test
    void publish_success_returnsOk() throws Exception {
        mockMvc.perform(post("/infra/mqtt/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"topic":"test/topic","payload":"hello","qos":1,"retained":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
