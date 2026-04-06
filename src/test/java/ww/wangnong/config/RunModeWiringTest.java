package ww.wangnong.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import ww.wangnong.bridge.AgriChatService;
import ww.wangnong.bridge.BridgeDeviceService;
import ww.wangnong.bridge.CommandAckTracker;
import ww.wangnong.mock.MockChatService;

class RunModeWiringTest {

    @Test
    @DisplayName("AgriChatService should activate in bridge/full")
    void agriChatService_condition_shouldCoverBridgeAndFull() {
        String expression = getConditionExpression(AgriChatService.class);
        assertThat(expression).contains("bridge");
        assertThat(expression).contains("full");
        assertThat(expression).doesNotContain("hardware");
    }

    @Test
    @DisplayName("MockChatService should activate in mock/hardware")
    void mockChatService_condition_shouldCoverMockAndHardware() {
        String expression = getConditionExpression(MockChatService.class);
        assertThat(expression).contains("mock");
        assertThat(expression).contains("hardware");
        assertThat(expression).doesNotContain("full");
    }

    @Test
    @DisplayName("BridgeDeviceService should activate in non-mock modes")
    void bridgeDeviceService_condition_shouldCoverBridgeHardwareFull() {
        String expression = getConditionExpression(BridgeDeviceService.class);
        assertThat(expression).contains("bridge");
        assertThat(expression).contains("hardware");
        assertThat(expression).contains("full");
    }

    @Test
    @DisplayName("CommandAckTracker should activate only in hardware/full")
    void commandAckTracker_condition_shouldCoverHardwareAndFull() {
        String expression = getConditionExpression(CommandAckTracker.class);
        assertThat(expression).contains("hardware");
        assertThat(expression).contains("full");
        assertThat(expression).doesNotContain("bridge");
    }

    private String getConditionExpression(Class<?> type) {
        ConditionalOnExpression annotation = type.getAnnotation(ConditionalOnExpression.class);
        assertThat(annotation)
                .as("%s should declare @ConditionalOnExpression", type.getSimpleName())
                .isNotNull();
        return annotation.value();
    }
}
