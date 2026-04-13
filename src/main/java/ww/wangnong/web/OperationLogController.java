package ww.wangnong.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.OperationLogService;

/**
 * Purpose: 提供后台操作审计日志查询接口。
 * Responsibilities: 接收分页与筛选条件，并将其透传给 OperationLogService 做统一过滤。
 * Flow: GET 请求 -> 读取分页/筛选 query param -> OperationLogService.listFiltered ->
 * ApiResponse.ok。
 * Dependencies: OperationLogService、ApiResponse。
 * Input: 页码、页大小、类型、目标标识和时间范围。
 * Output: 审计日志分页结果。
 */
@RestController
@RequestMapping("/v1/admin/logs")
public class OperationLogController {

    private final OperationLogService logService;

    public OperationLogController(OperationLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(logService.listFiltered(page, size, type, targetId, startDate, endDate));
    }
}
