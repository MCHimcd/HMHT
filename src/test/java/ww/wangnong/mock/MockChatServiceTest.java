package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ww.wangnong.service.ChatService.ChatResult;

class MockChatServiceTest {

    private final MockChatService service = new MockChatService();

    @Test
    void chat_withMessage_returnsReplyContainingMessage() {
        ChatResult result = service.chat("如何种植番茄？", null);

        assertThat(result.reply()).contains("如何种植番茄？");
        assertThat(result.reply()).contains("模拟模式");
    }
}
