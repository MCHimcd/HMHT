package ww.wangnong.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.config.condition.ConditionalOnMockMode;
import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.ExtraService;

@RestController
@ConditionalOnMockMode
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
