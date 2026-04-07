package ww.wangnong.bridge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import ww.wangnong.config.condition.ConditionalOnPythonRoute;
import ww.wangnong.service.ChatService;

/**
 * Chat service implementation that proxies requests to the Python agri backend.
 * Python endpoint: POST /api/multi-conversation/chat (multipart/form-data)
 * 
 * 在 Python 路由启用时激活（bridge/full 模式）。
 */
@Service
@Primary
@ConditionalOnPythonRoute
public class AgriChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(AgriChatService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RestClient restClient;

    /** Currently we map a fixed user to one session. */
    private final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();
    private static final String DEFAULT_USER = "default";

    public AgriChatService(@Value("${hmht.agri.chat.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ChatResult chat(String message, String imageUri) {
        try {
            String sessionId = ensureSession(DEFAULT_USER);

            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("session_id", sessionId);
            parts.add("message", message != null ? message : "");

            // If imageUri is a local file path, attach it as multipart file
            if (imageUri != null && !imageUri.isBlank()) {
                File imageFile = new File(imageUri);
                if (imageFile.isFile()) {
                    byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                    parts.add("image", new ByteArrayResource(imageBytes) {
                        @Override
                        public String getFilename() {
                            return imageFile.getName();
                        }
                    });
                } else {
                    log.warn("[AgriChat] Image path not a valid file: {}", imageUri);
                }
            }

            String responseBody = restClient.post()
                    .uri("/api/multi-conversation/chat")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> resp = MAPPER.readValue(responseBody, MAP_TYPE);
            String reply = String.valueOf(resp.getOrDefault("reply", "AI 暂无回复"));
            return new ChatResult(reply);

        } catch (Exception e) {
            log.error("[AgriChat] Chat request failed", e);
            return new ChatResult("AI 服务暂时不可用，请稍后重试。错误: " + e.getMessage());
        }
    }

    private String ensureSession(String userId) {
        return userSessions.computeIfAbsent(userId, k -> {
            try {
                String resp = restClient.post()
                        .uri("/api/multi-conversation/session/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}")
                        .retrieve()
                        .body(String.class);
                Map<String, Object> result = MAPPER.readValue(resp, MAP_TYPE);
                String sid = String.valueOf(result.get("session_id"));
                log.info("[AgriChat] Created new session: {}", sid);
                return sid;
            } catch (IOException e) {
                throw new RuntimeException("Failed to create chat session", e);
            }
        });
    }
}
