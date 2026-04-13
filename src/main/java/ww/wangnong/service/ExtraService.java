package ww.wangnong.service;

import java.util.List;

/**
 * Purpose: 定义溯源与摄像头扩展能力契约。
 * Responsibilities: 统一扫码、直播、回放和关键帧查询的返回模型。
 * Flow: 控制器提交节点或溯源码 -> 实现类从 mock 或外部系统取数 -> 返回前端所需 record。
 * Dependencies: 具体实现中的 mock 数据源或外部视频平台。
 * Input: 溯源码、节点标识、日期。
 * Output: 溯源链路、直播信息、回放列表、关键帧列表。
 */
public interface ExtraService {

        record TraceabilityStep(String stepName, String operator, String time,
                        String location, String remark) {
        }

        record TraceabilityResult(String productName, String batchCode, String variety,
                        String origin, String farmName, String plotId,
                        String plantDate, String harvestDate, String shelfLife,
                        String qualityGrade, String certNo, String inspectorName,
                        String inspectDate, List<TraceabilityStep> steps) {
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
