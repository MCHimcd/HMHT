package ww.wangnong.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import ww.wangnong.config.condition.ConditionalOnMockMode;
import ww.wangnong.service.ExtraService;

@Service
@ConditionalOnMockMode
public class MockExtraService implements ExtraService {

    @Override
    public TraceabilityResult scanTraceability(String code) {
        return new TraceabilityResult("溯源码 " + (code == null ? "" : code) + " 已记录");
    }

    @Override
    public LiveStreamInfo getCameraLive(String nodeId) {
        return new LiveStreamInfo(
                nodeId,
                "温室摄像头 " + nodeId,
                "online",
                "HLS",
                "1920x1080",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "https://example.com/live/" + nodeId + ".m3u8");
    }

    @Override
    public List<PlaybackItem> getCameraPlayback(String nodeId, String date) {
        String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        return List.of(
                new PlaybackItem("pb-001", d, "00:30:00", "1080p", true, 1,
                        "https://example.com/playback/" + nodeId + "/" + d + "/001.mp4"),
                new PlaybackItem("pb-002", d, "00:20:00", "720p", true, 0,
                        "https://example.com/playback/" + nodeId + "/" + d + "/002.mp4"));
    }

    @Override
    public List<KeyframeItem> getCameraKeyframes(String nodeId, String date) {
        String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        return List.of(
                new KeyframeItem("kf-001", d + "T09:30:00", "叶片发黄", 0.87,
                        "thumb://" + nodeId + "/1", true, "叶片异常", "warning",
                        "https://example.com/keyframes/" + nodeId + "/" + d + "/kf-001.jpg"),
                new KeyframeItem("kf-002", d + "T14:20:00", "虫害疑似", 0.78,
                        "thumb://" + nodeId + "/2", true, "虫害预警", "critical",
                        "https://example.com/keyframes/" + nodeId + "/" + d + "/kf-002.jpg"));
    }
}
