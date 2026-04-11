package ww.wangnong.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.service.OperationLogService;
import ww.wangnong.service.OperationLogService.LogItem;
import ww.wangnong.service.OperationLogService.LogPage;

@WebMvcTest(OperationLogController.class)
@Import(SecurityConfig.class)
class OperationLogControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private OperationLogService logService;

        @Test
        void list_defaultParams_returnsPage() throws Exception {
                when(logService.listFiltered(1, 20, null, null, null, null))
                                .thenReturn(new LogPage(1, List.of(
                                                new LogItem(1L, 1L, "用户1", "LOGIN", "user", "1",
                                                                "登录成功", "2026-01-01T00:00:00"))));

                mockMvc.perform(get("/v1/admin/logs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.total").value(1));
        }

        @Test
        void list_withFilter_returnsFiltered() throws Exception {
                when(logService.listFiltered(1, 10, "LOGIN", null, null, null))
                                .thenReturn(new LogPage(1, List.of(
                                                new LogItem(1L, 1L, "用户1", "LOGIN", "user", "1",
                                                                "登录成功", "2026-01-01T00:00:00"))));

                mockMvc.perform(get("/v1/admin/logs")
                                .param("page", "1")
                                .param("size", "10")
                                .param("type", "LOGIN"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }
}
