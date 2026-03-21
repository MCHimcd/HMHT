package ww.wangnong.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.ChatService;

@RestController
@RequestMapping("/v1/agri-qa")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    record ChatRequest(String message, @JsonProperty("image_uri") String imageUri) {}

    @PostMapping("/chat")
    public ApiResponse<?> chat(@RequestBody ChatRequest req) {
        return ApiResponse.ok(chatService.chat(req.message(), req.imageUri()));
    }
}
