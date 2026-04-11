package ww.wangnong.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ww.wangnong.service.ChatService.ChatResult;

class AgriChatServiceTest {

    private MockRestServiceServer mockServer;
    private AgriChatService service;

    @BeforeEach
    void setUp() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://test-agri");
        mockServer = MockRestServiceServer.bindTo(builder).build();

        service = new AgriChatService("http://test-agri");
        // Replace the internally-created RestClient with the mock-backed one
        Field restClientField = AgriChatService.class.getDeclaredField("restClient");
        restClientField.setAccessible(true);
        restClientField.set(service, builder.build());
    }

    @Test
    void chat_successfulReply_returnsChatResult() {
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/session/new"))
                .andRespond(withSuccess("{\"session_id\":\"S1\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/chat"))
                .andRespond(withSuccess("{\"reply\":\"Hello from AI\"}", MediaType.APPLICATION_JSON));

        ChatResult result = service.chat("Hi", null);

        assertThat(result.reply()).isEqualTo("Hello from AI");
        mockServer.verify();
    }

    @Test
    void chat_emptyReply_fallbackText() {
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/session/new"))
                .andRespond(withSuccess("{\"session_id\":\"S1\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/chat"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        ChatResult result = service.chat("Hi", null);

        assertThat(result.reply()).isEqualTo("AI 暂无回复");
    }

    @Test
    void chat_sessionCreatedOnce_reusedOnSecondCall() {
        // First call: session create + chat
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/session/new"))
                .andRespond(withSuccess("{\"session_id\":\"S1\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/chat"))
                .andRespond(withSuccess("{\"reply\":\"R1\"}", MediaType.APPLICATION_JSON));
        // Second call: only chat (session already cached)
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/chat"))
                .andRespond(withSuccess("{\"reply\":\"R2\"}", MediaType.APPLICATION_JSON));

        service.chat("Q1", null);
        ChatResult result = service.chat("Q2", null);

        assertThat(result.reply()).isEqualTo("R2");
        mockServer.verify();
    }

    @Test
    void chat_chatEndpointError_returnsFriendlyMessage() {
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/session/new"))
                .andRespond(withSuccess("{\"session_id\":\"S1\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/chat"))
                .andRespond(withServerError());

        ChatResult result = service.chat("Hi", null);

        assertThat(result.reply()).contains("AI 服务暂时不可用");
    }

    @Test
    void chat_sessionCreateError_returnsFriendlyMessage() {
        mockServer.expect(requestTo("http://test-agri/api/multi-conversation/session/new"))
                .andRespond(withServerError());

        ChatResult result = service.chat("Hi", null);

        assertThat(result.reply()).contains("AI 服务暂时不可用");
    }
}
