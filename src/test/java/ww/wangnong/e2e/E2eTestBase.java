package ww.wangnong.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
abstract class E2eTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Login with the given credentials and return the token.
     */
    protected String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode root = MAPPER.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    /**
     * Send verification code and return the code (exposed in dev mode).
     */
    protected String sendCodeAndGetCode(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"phone":"%s"}""".formatted(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode root = MAPPER.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("test_code").asText();
    }
}
