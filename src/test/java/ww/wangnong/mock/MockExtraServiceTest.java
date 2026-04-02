package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ww.wangnong.service.ExtraService.KeyframeItem;
import ww.wangnong.service.ExtraService.LiveStreamInfo;
import ww.wangnong.service.ExtraService.PlaybackItem;
import ww.wangnong.service.ExtraService.TraceabilityResult;

class MockExtraServiceTest {

    private final MockExtraService service = new MockExtraService();

    @Test
    void scanTraceability_returnsTextContainingCode() {
        TraceabilityResult result = service.scanTraceability("TRACE-001");

        assertThat(result.text()).contains("TRACE-001");
    }

    @Test
    void getCameraLive_returnsOnlineStream() {
        LiveStreamInfo result = service.getCameraLive("N01");

        assertThat(result.cameraId()).isEqualTo("N01");
        assertThat(result.status()).isEqualTo("online");
        assertThat(result.streamUrl()).contains("N01");
    }

    @Test
    void getCameraPlayback_returnsItems() {
        List<PlaybackItem> result = service.getCameraPlayback("N01", "2026-01-01");

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().date()).isEqualTo("2026-01-01");
    }

    @Test
    void getCameraKeyframes_returnsAbnormalItems() {
        List<KeyframeItem> result = service.getCameraKeyframes("N01", null);

        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(KeyframeItem::isAbnormal);
    }
}
