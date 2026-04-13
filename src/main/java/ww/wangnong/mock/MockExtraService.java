package ww.wangnong.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import ww.wangnong.service.ExtraService;

/**
 * Purpose: 在 mock 模式下提供溯源与摄像头扩展功能的演示数据。
 * Responsibilities: 返回固定的溯源链路、直播信息、回放和关键帧列表，保证前端可联调完整页面。
 * Flow: 控制器调用各查询方法 -> 本类直接构造 record 结果返回。
 * Dependencies: ExtraService。
 * Input: 溯源码、节点标识、日期。
 * Output: 固定但结构完整的扩展能力数据。
 */
@Service
public class MockExtraService implements ExtraService {

        @Override
        public TraceabilityResult scanTraceability(String code) {
                String batchCode = (code == null || code.isBlank()) ? "WN-2026-A03-0412" : code;
                return new TraceabilityResult(
                                "有机番茄",
                                batchCode,
                                "粉果·千禧小番茄",
                                "山东省寿光市",
                                "鸿蒙慧田智慧农场",
                                "A03 温室大棚",
                                "2026-01-15",
                                "2026-04-10",
                                "常温7天 / 冷藏15天",
                                "绿色A级",
                                "GR-2026-SD-003721",
                                "张明远",
                                "2026-04-11",
                                List.of(
                                                new TraceabilityStep("播种育苗", "李大田",
                                                                "2026-01-15 08:30",
                                                                "A03 育苗室",
                                                                "基质穴盘128孔，发芽率96%"),
                                                new TraceabilityStep("定植移栽", "李大田",
                                                                "2026-02-10 09:00",
                                                                "A03 温室大棚",
                                                                "株距40cm，行距60cm"),
                                                new TraceabilityStep("生长管理", "王慧农",
                                                                "2026-02-10 ~ 2026-04-08",
                                                                "A03 温室大棚",
                                                                "智能灌溉+LED补光，AI病虫害监测0次告警"),
                                                new TraceabilityStep("采收入库", "王慧农",
                                                                "2026-04-10 06:45",
                                                                "A03 采后处理间",
                                                                "人工采摘，即日分拣入库"),
                                                new TraceabilityStep("质量检测", "张明远",
                                                                "2026-04-11 10:00",
                                                                "寿光农检中心",
                                                                "农残未检出，糖度9.2°Brix，合格"),
                                                new TraceabilityStep("包装出库", "赵小禾",
                                                                "2026-04-12 07:00",
                                                                "冷链仓储中心",
                                                                "冷链运输，目标温度4℃")));
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
                                "/api/mock-media/live-greenhouse.mp4");
        }

        @Override
        public List<PlaybackItem> getCameraPlayback(String nodeId, String date) {
                String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
                return List.of(
                                new PlaybackItem("pb-001", d, "00:30:00", "1080p", true, 1,
                                                "/api/mock-media/playback-morning.mp4"),
                                new PlaybackItem("pb-002", d, "00:20:00", "720p", true, 0,
                                                "/api/mock-media/playback-afternoon.mp4"));
        }

        @Override
        public List<KeyframeItem> getCameraKeyframes(String nodeId, String date) {
                String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
                return List.of(
                                new KeyframeItem("kf-001", d + "T09:30:00", "叶片病害", 95,
                                                "thumb://" + nodeId + "/1", true, "叶片病害", "critical",
                                                "/api/mock-media/keyframe-leaf-disease.jpg"),
                                new KeyframeItem("kf-002", d + "T14:20:00", "虫害检测", 92,
                                                "thumb://" + nodeId + "/2", true, "虫害检测", "critical",
                                                "/api/mock-media/keyframe-pest-detect.jpg"));
        }
}
