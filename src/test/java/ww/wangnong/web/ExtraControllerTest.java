package ww.wangnong.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import org.springframework.test.context.TestPropertySource;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.service.ExtraService;
import ww.wangnong.service.ExtraService.KeyframeItem;
import ww.wangnong.service.ExtraService.LiveStreamInfo;
import ww.wangnong.service.ExtraService.PlaybackItem;
import ww.wangnong.service.ExtraService.TraceabilityResult;

@WebMvcTest(ExtraController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "hmht.run.mode=mock")
class ExtraControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ExtraService extraService;

        @Test
        void scanTraceability_returnsResult() throws Exception {
                when(extraService.scanTraceability("CODE-001"))
                                .thenReturn(new TraceabilityResult("溯源码 CODE-001 已记录"));

                mockMvc.perform(post("/v1/traceability/scan")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"code":"CODE-001"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.text").value("溯源码 CODE-001 已记录"));
        }

        @Test
        void getCameraLive_returnsStreamInfo() throws Exception {
                when(extraService.getCameraLive("N01"))
                                .thenReturn(new LiveStreamInfo("N01", "摄像头", "online",
                                                "HLS", "1080p", "2026-01-01T00:00:00",
                                                "https://example.com/live/N01.m3u8"));

                mockMvc.perform(get("/v1/cameras/N01/live"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.cameraId").value("N01"));
        }

        @Test
        void getPlayback_returnsItems() throws Exception {
                when(extraService.getCameraPlayback("N01", null))
                                .thenReturn(List.of(new PlaybackItem("pb-001", "2026-01-01",
                                                "00:30:00", "1080p", true, 0)));

                mockMvc.perform(get("/v1/cameras/N01/playback"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data[0].id").value("pb-001"));
        }

        @Test
        void getKeyframes_returnsItems() throws Exception {
                when(extraService.getCameraKeyframes("N01", null))
                                .thenReturn(List.of(new KeyframeItem("kf-001", "2026-01-01T09:30:00",
                                                "叶片发黄", 0.87, "thumb://N01/1", true, "叶片异常", "warning")));

                mockMvc.perform(get("/v1/cameras/N01/keyframes"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data[0].id").value("kf-001"));
        }
}
