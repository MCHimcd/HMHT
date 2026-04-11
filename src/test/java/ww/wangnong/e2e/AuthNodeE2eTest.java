package ww.wangnong.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthNodeE2eTest extends E2eTestBase {

    @Test
    void registerLoginAndListNodes_fullFlow() throws Exception {
        // 1) Send verification code
        String code = sendCodeAndGetCode("13800099001");

        // 2) Register
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"phone":"13800099001","verificationCode":"%s","password":"pwd123"}"""
                        .formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 3) Login
        String token = login("13800099001", "pwd123");

        // 4) List nodes (seeded by EssentialDataSeeder)
        mockMvc.perform(get("/v1/nodes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void loginWithWrongPassword_returnsBizError() throws Exception {
        // admin user seeded by EssentialDataSeeder
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"13800000001","password":"WRONG"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
