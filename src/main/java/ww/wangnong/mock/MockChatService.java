package ww.wangnong.mock;

import org.springframework.stereotype.Service;
import ww.wangnong.config.condition.ConditionalOnNoPythonRoute;
import ww.wangnong.service.ChatService;

/**
 * Purpose: 在没有 Python 后端时提供最小可用的问答占位实现。
 * Responsibilities: 返回确定性的 mock 文本，保证 ChatController 与前端流程可联调。
 * Flow: Controller 调用 chat -> 直接拼接输入消息 -> 返回 ChatResult。
 * Dependencies: ConditionalOnNoPythonRoute、ChatService。
 * Input: 用户消息和可选图片参数。
 * Output: 简单的 mock 回复文本。
 */
@Service
@ConditionalOnNoPythonRoute
public class MockChatService implements ChatService {

    @Override
    public ChatResult chat(String message, String imageUri, String imageBase64) {
        return new ChatResult("Mock response for: " + message);
    }
}
