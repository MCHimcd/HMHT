package ww.wangnong.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ScheduledTaskE2eTest extends E2eTestBase {

    @Test
    void taskCrud_fullFlow() throws Exception {
        // 1) Login as admin (seeded by EssentialDataSeeder)
        String token = login("13800000001", "admin123");

        // 2) Create task
        MvcResult createResult = mockMvc.perform(post("/v1/tasks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nodeId":"SIM_Node_01","equipmentKey":"water_pump","action":"turn_on",
                         "taskName":"E2E灌溉","hour":8,"minute":0,"repeatDays":"1,2,3,4,5"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        JsonNode data = MAPPER.readTree(createResult.getResponse().getContentAsString())
                .path("data");
        long taskId = data.path("id").asLong();

        // 3) List tasks — should contain the new task
        mockMvc.perform(get("/v1/tasks")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %d)]".formatted(taskId)).exists());

        // 4) Update task name
        mockMvc.perform(put("/v1/tasks/" + taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nodeId":"SIM_Node_01","equipmentKey":"water_pump","action":"turn_on",
                         "taskName":"E2E灌溉-修改","hour":9,"minute":30,"repeatDays":"1,2,3"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 5) Toggle disable
        mockMvc.perform(put("/v1/tasks/" + taskId + "/toggle")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 6) Delete task
        mockMvc.perform(delete("/v1/tasks/" + taskId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 7) List again — should be empty for this user
        mockMvc.perform(get("/v1/tasks")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
