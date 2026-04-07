package ww.wangnong.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Conditional;

import ww.wangnong.bridge.AgriChatService;
import ww.wangnong.bridge.CommandAckTracker;
import ww.wangnong.bridge.MqttBridge;
import ww.wangnong.bridge.ProtocolPersistenceService;
import ww.wangnong.config.condition.ConditionalOnBridgeMode;
import ww.wangnong.config.condition.ConditionalOnHardwareRoute;
import ww.wangnong.config.condition.ConditionalOnNoPythonRoute;
import ww.wangnong.config.condition.ConditionalOnPythonRoute;
import ww.wangnong.mock.MockChatService;

/**
 * 验证各组件使用了正确的条件注解。
 */
class RunModeWiringTest {

    @Test
    @DisplayName("AgriChatService should use @ConditionalOnPythonRoute (bridge/full)")
    void agriChatService_usesCorrectCondition() {
        assertThat(AgriChatService.class.isAnnotationPresent(ConditionalOnPythonRoute.class))
                .as("AgriChatService should have @ConditionalOnPythonRoute")
                .isTrue();
    }

    @Test
    @DisplayName("MockChatService should use @ConditionalOnNoPythonRoute (mock/hardware)")
    void mockChatService_usesCorrectCondition() {
        assertThat(MockChatService.class.isAnnotationPresent(ConditionalOnNoPythonRoute.class))
                .as("MockChatService should have @ConditionalOnNoPythonRoute")
                .isTrue();
    }

    @Test
    @DisplayName("MqttBridge should use @ConditionalOnBridgeMode (bridge/hardware/full)")
    void mqttBridge_usesCorrectCondition() {
        assertThat(MqttBridge.class.isAnnotationPresent(ConditionalOnBridgeMode.class))
                .as("MqttBridge should have @ConditionalOnBridgeMode")
                .isTrue();
    }

    @Test
    @DisplayName("ProtocolPersistenceService should use @ConditionalOnBridgeMode (bridge/hardware/full)")
    void protocolPersistenceService_usesCorrectCondition() {
        assertThat(ProtocolPersistenceService.class.isAnnotationPresent(ConditionalOnBridgeMode.class))
                .as("ProtocolPersistenceService should have @ConditionalOnBridgeMode")
                .isTrue();
    }

    @Test
    @DisplayName("CommandAckTracker should use @ConditionalOnHardwareRoute (hardware/full)")
    void commandAckTracker_usesCorrectCondition() {
        assertThat(CommandAckTracker.class.isAnnotationPresent(ConditionalOnHardwareRoute.class))
                .as("CommandAckTracker should have @ConditionalOnHardwareRoute")
                .isTrue();
    }

    @Test
    @DisplayName("RunMode enum should correctly identify Python route modes")
    void runMode_pythonRouteIdentification() {
        assertThat(RunMode.BRIDGE.isPythonRouteEnabled()).isTrue();
        assertThat(RunMode.FULL.isPythonRouteEnabled()).isTrue();
        assertThat(RunMode.MOCK.isPythonRouteEnabled()).isFalse();
        assertThat(RunMode.HARDWARE.isPythonRouteEnabled()).isFalse();
    }

    @Test
    @DisplayName("RunMode enum should correctly identify Hardware route modes")
    void runMode_hardwareRouteIdentification() {
        assertThat(RunMode.HARDWARE.isHardwareRouteEnabled()).isTrue();
        assertThat(RunMode.FULL.isHardwareRouteEnabled()).isTrue();
        assertThat(RunMode.MOCK.isHardwareRouteEnabled()).isFalse();
        assertThat(RunMode.BRIDGE.isHardwareRouteEnabled()).isFalse();
    }

    @Test
    @DisplayName("RunMode enum should correctly identify Mock data mode")
    void runMode_mockDataIdentification() {
        assertThat(RunMode.MOCK.isMockDataEnabled()).isTrue();
        assertThat(RunMode.BRIDGE.isMockDataEnabled()).isFalse();
        assertThat(RunMode.HARDWARE.isMockDataEnabled()).isFalse();
        assertThat(RunMode.FULL.isMockDataEnabled()).isFalse();
    }

    @Test
    @DisplayName("RunMode.fromString should parse valid modes")
    void runMode_fromString_validModes() {
        assertThat(RunMode.fromString("mock")).isEqualTo(RunMode.MOCK);
        assertThat(RunMode.fromString("MOCK")).isEqualTo(RunMode.MOCK);
        assertThat(RunMode.fromString("bridge")).isEqualTo(RunMode.BRIDGE);
        assertThat(RunMode.fromString("hardware")).isEqualTo(RunMode.HARDWARE);
        assertThat(RunMode.fromString("full")).isEqualTo(RunMode.FULL);
    }

    @Test
    @DisplayName("RunMode.fromString should default to MOCK for invalid values")
    void runMode_fromString_invalidDefaultsToMock() {
        assertThat(RunMode.fromString(null)).isEqualTo(RunMode.MOCK);
        assertThat(RunMode.fromString("")).isEqualTo(RunMode.MOCK);
        assertThat(RunMode.fromString("invalid")).isEqualTo(RunMode.MOCK);
    }
}
