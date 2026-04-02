package ww.wangnong.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import ww.wangnong.service.ScheduledTaskService;
import ww.wangnong.service.ScheduledTaskService.TaskInfo;

@WebMvcTest(ScheduledTaskController.class)
@Import(SecurityConfig.class)
class ScheduledTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduledTaskService taskService;

    private TaskInfo stubTask() {
        return new TaskInfo(1L, "N01", "water_pump", "turn_on",
                "定时灌溉", 8, 0, "1,2,3,4,5", true, 1700000000000L);
    }

    @Test
    void list_returnsTasks() throws Exception {
        when(taskService.listByUser("token")).thenReturn(List.of(stubTask()));

        mockMvc.perform(get("/v1/tasks")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void create_returnsCreatedTask() throws Exception {
        when(taskService.create(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(stubTask());

        mockMvc.perform(post("/v1/tasks")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nodeId":"N01","equipmentKey":"water_pump","action":"turn_on",
                         "taskName":"灌溉","hour":8,"minute":0,"repeatDays":"1,2,3"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void update_returnsUpdatedTask() throws Exception {
        when(taskService.update(anyString(), anyLong(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(stubTask());

        mockMvc.perform(put("/v1/tasks/1")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nodeId":"N01","equipmentKey":"water_pump","action":"turn_on",
                         "taskName":"灌溉","hour":9,"minute":30,"repeatDays":"1,2,3"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/v1/tasks/1")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void toggle_returnsOk() throws Exception {
        mockMvc.perform(put("/v1/tasks/1/toggle")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
