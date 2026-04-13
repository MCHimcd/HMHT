package ww.wangnong.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.ExtraService;

/**
 * Purpose: 提供溯源扫码与摄像头扩展能力接口。
 * Responsibilities: 统一封装与农业主链路无强耦合的附加功能入口，保持控制器只负责参数转发。
 * Flow: HTTP 请求 -> 解析扫码码值或节点参数 -> ExtraService -> ApiResponse.ok。
 * Dependencies: ExtraService、ApiResponse。
 * Input: 溯源码、节点标识、可选日期。
 * Output: 溯源结果、实时视频地址、回放或关键帧数据。
 */
@RestController
public class ExtraController {

    private final ExtraService extraService;

    public ExtraController(ExtraService extraService) {
        this.extraService = extraService;
    }

    record TraceabilityScanRequest(String code) {
    }

    @PostMapping("/v1/traceability/scan")
    public ApiResponse<?> scanTraceability(@RequestBody TraceabilityScanRequest req) {
        return ApiResponse.ok(extraService.scanTraceability(req.code()));
    }

    @GetMapping("/v1/cameras/{nodeId}/live")
    public ApiResponse<?> getCameraLive(@PathVariable String nodeId) {
        return ApiResponse.ok(extraService.getCameraLive(nodeId));
    }

    @GetMapping("/v1/cameras/{nodeId}/playback")
    public ApiResponse<?> getPlayback(@PathVariable String nodeId,
            @RequestParam(required = false) String date) {
        return ApiResponse.ok(extraService.getCameraPlayback(nodeId, date));
    }

    @GetMapping("/v1/cameras/{nodeId}/keyframes")
    public ApiResponse<?> getKeyframes(@PathVariable String nodeId,
            @RequestParam(required = false) String date) {
        return ApiResponse.ok(extraService.getCameraKeyframes(nodeId, date));
    }
}
