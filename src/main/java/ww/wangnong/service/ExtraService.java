package ww.wangnong.service;

import java.util.List;

public interface ExtraService {

    record TraceabilityResult(String text) {
    }

    record LiveStreamInfo(String cameraId, String cameraName, String status,
            String streamTag, String resolution,
            String lastHeartbeat, String streamUrl) {
    }

    record PlaybackItem(String id, String date, String duration,
            String quality, boolean available, int abnormalCount,
            String videoUrl) {
    }

    record KeyframeItem(String id, String captureTime, String label,
            double confidence, String thumbnailHint,
            boolean isAbnormal, String abnormalType, String severity,
            String imageUrl) {
    }

    TraceabilityResult scanTraceability(String code);

    LiveStreamInfo getCameraLive(String nodeId);

    List<PlaybackItem> getCameraPlayback(String nodeId, String date);

    List<KeyframeItem> getCameraKeyframes(String nodeId, String date);
}
