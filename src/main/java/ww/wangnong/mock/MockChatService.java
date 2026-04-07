package ww.wangnong.mock;

import org.springframework.stereotype.Service;

import ww.wangnong.config.condition.ConditionalOnNoPythonRoute;
import ww.wangnong.service.ChatService;

/**
 * ChatService 的 Mock 实现，在没有 Python 后端时使用（mock/hardware 模式）。
 */
@Service
@ConditionalOnNoPythonRoute
public class MockChatService implements ChatService {

    @Override
    public ChatResult chat(String message, String imageUri) {
        String reply = "您好！这是模拟模式的自动回复。\n\n"
                + "您的问题是：" + (message != null ? message : "(无文字)") + "\n\n"
                + "在生产环境中，此接口会将您的问题转发给 AI 大模型进行专业农业知识解答。";
        return new ChatResult(reply);
    }
}
