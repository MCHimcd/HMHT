package ww.wangnong.bridge;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ww.wangnong.config.condition.ConditionalOnPythonRoute;
import ww.wangnong.service.ChatService;

/**
 * Purpose: 将农业问答请求代理到 agri Python 后端。
 * Responsibilities: 维护会话 ID、转发文本/图片请求，并把 Python 返回结果收敛为 ChatService 契约。
 * Flow: chat -> ensureSession -> POST Python chat API -> 解析 reply -> 返回
 * ChatResult。
 * Dependencies: RestClient、Python multi-conversation
 * API、ConditionalOnPythonRoute。
 * Input: 文本问题、图片 URI、图片 base64。
 * Output: agri 后端生成的问答回复。
 * Boundary: 当前实现把默认用户固定映射到一个 session，适合单用户演示，不是多用户隔离方案。
 */
@Service
@Primary
@ConditionalOnPythonRoute
public class AgriChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(AgriChatService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestClient restClient;

    private final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();
    private static final String DEFAULT_USER = "default";

    public AgriChatService(@Value("${hmht.agri.chat.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ChatResult chat(String message, String imageUri, String imageBase64) {
        try {
            String sessionId = ensureSession(DEFAULT_USER);

            // Build JSON request body
            Map<String, Object> body = new HashMap<>();
            body.put("session_id", sessionId);
            body.put("message", message != null ? message : "");

            // Include Base64 image if provided
            if (imageBase64 != null && !imageBase64.isBlank()) {
                body.put("image_base64", imageBase64);
                log.debug("[AgriChat] Forwarding Base64 image, size: {} chars", imageBase64.length());
            } else if (imageUri != null && !imageUri.isBlank()) {
                // For backward compatibility, log if imageUri is provided without Base64
                log.warn("[AgriChat] Image URI provided without Base64 (old format): {}", imageUri);
            }

            String responseBody = restClient.post()
                    .uri("/api/multi-conversation/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
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
                Map<String, Object> body = new HashMap<>();
                String resp = restClient.post()
                        .uri("/api/multi-conversation/session/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);
                Map<String, Object> result = MAPPER.readValue(resp, MAP_TYPE);
                String sid = String.valueOf(result.get("session_id"));
                log.info("[AgriChat] Created new session: {}", sid);
                return sid;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create chat session", e);
            }
        });
    }
}
