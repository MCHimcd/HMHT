package ww.wangnong.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.service.NodeService;
import ww.wangnong.service.NodeService.NodeDetail;
import ww.wangnong.service.NodeService.NodeInfo;

@WebMvcTest(NodeController.class)
@Import(SecurityConfig.class)
class NodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NodeService nodeService;

    @Test
    void listNodes_returnsNodeList() throws Exception {
        when(nodeService.listNodes("token"))
                .thenReturn(List.of(new NodeInfo("N01", "温室1", "online")));

        mockMvc.perform(get("/v1/nodes")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("N01"));
    }

    @Test
    void getNode_returnsNodeDetail() throws Exception {
        when(nodeService.getNode("token", "N01"))
                .thenReturn(new NodeDetail("N01", "温室1", "online", Map.of("smartMode", false)));

        mockMvc.perform(get("/v1/nodes/N01")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("N01"));
    }

    @Test
    void getSensorMeta_returnsList() throws Exception {
        when(nodeService.getSensorMeta("N01"))
                .thenReturn(List.of(new NodeService.SensorMetaInfo(
                        "temperature", "温度", "℃", null, null, null, 10.0, 35.0)));

        mockMvc.perform(get("/v1/nodes/N01/sensor-meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].key").value("temperature"));
    }

    @Test
    void getEquipmentMeta_returnsList() throws Exception {
        when(nodeService.getEquipmentMeta("N01"))
                .thenReturn(List.of(new NodeService.EquipmentMetaInfo(
                        "water_pump", "水泵", null, true)));

        mockMvc.perform(get("/v1/nodes/N01/equipment-meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].key").value("water_pump"));
    }
}
