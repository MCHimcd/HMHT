package ww.wangnong.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.ChatService;

/**
 * Purpose: 暴露农业问答入口，兼容纯文本与图像增强问答。
 * Responsibilities: 解析问答请求体并透传给 ChatService，不在控制器层做模型或路由分支判断。
 * Flow: HTTP POST -> 读取 message/image 字段 -> ChatService.chat -> ApiResponse.ok。
 * Dependencies: ChatService、Jackson JSON 映射。
 * Input: 文本问题、可选图片 URI、可选图片 base64。
 * Output: 对话应答数据。
 */
@RestController
@RequestMapping("/v1/agri-qa")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    record ChatRequest(
            String message,
            @JsonProperty("image_uri") String imageUri,
            @JsonProperty("image_base64") String imageBase64) {
    }

    @PostMapping("/chat")
    public ApiResponse<?> chat(@RequestBody ChatRequest req) {
        return ApiResponse.ok(chatService.chat(req.message(), req.imageUri(), req.imageBase64()));
    }
}
