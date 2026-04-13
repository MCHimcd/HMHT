package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ww.wangnong.service.ExtraService.KeyframeItem;
import ww.wangnong.service.ExtraService.LiveStreamInfo;
import ww.wangnong.service.ExtraService.PlaybackItem;
import ww.wangnong.service.ExtraService.TraceabilityResult;
import ww.wangnong.service.ExtraService.TraceabilityStep;

class MockExtraServiceTest {

    private final MockExtraService service = new MockExtraService();

    @Test
    void scanTraceability_returnsFullInfo() {
        TraceabilityResult result = service.scanTraceability("WN-2026-A03-0412");

        assertThat(result.productName()).isEqualTo("有机番茄");
        assertThat(result.batchCode()).isEqualTo("WN-2026-A03-0412");
        assertThat(result.steps()).isNotEmpty();
        assertThat(result.steps().getFirst().stepName()).isEqualTo("播种育苗");
    }

    @Test
    void getCameraLive_returnsOnlineStream() {
        LiveStreamInfo result = service.getCameraLive("N01");

        assertThat(result.cameraId()).isEqualTo("N01");
        assertThat(result.status()).isEqualTo("online");
        assertThat(result.streamUrl()).isNotBlank();
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
