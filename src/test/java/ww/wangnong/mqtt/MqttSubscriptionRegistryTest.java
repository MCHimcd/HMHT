package ww.wangnong.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MqttSubscriptionRegistryTest {

    @Mock
    private PahoMqttService mqttService;

    @Test
    void register_validTopic_addsDefinition() {
        MqttSubscriptionRegistry registry = new MqttSubscriptionRegistry();

        registry.register("/hardware/device/+/telemetry", 1, msg -> {
        });

        assertThat(registry.snapshot()).hasSize(1);
        assertThat(registry.snapshot().getFirst().topicFilter()).isEqualTo("/hardware/device/+/telemetry");
        assertThat(registry.snapshot().getFirst().qos()).isEqualTo(1);
    }

    @Test
    void register_blankTopic_throwsIllegalArgumentException() {
        MqttSubscriptionRegistry registry = new MqttSubscriptionRegistry();

        assertThatThrownBy(() -> registry.register(" ", 1, msg -> {
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topicFilter");
    }

    @Test
    void bindAll_subscribesAllRegisteredDefinitions() {
        MqttSubscriptionRegistry registry = new MqttSubscriptionRegistry();
        registry.register("/hardware/device/+/telemetry", 1, msg -> {
        });
        registry.register("/hardware/device/+/ack", 0, msg -> {
        });

        registry.bindAll(mqttService);

        verify(mqttService).subscribe(eq("/hardware/device/+/telemetry"), eq(1), any());
        verify(mqttService).subscribe(eq("/hardware/device/+/ack"), eq(0), any());
    }
}
