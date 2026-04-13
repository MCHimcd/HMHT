package ww.wangnong.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ww.wangnong.dto.ApiResponse;

/**
 * Purpose: 将后端业务异常统一收敛为前端约定的响应格式。
 * Responsibilities: 捕获 BizException，保留业务 code/message，并避免控制器逐个处理异常转换。
 * Flow: Spring MVC 抛出 BizException -> 本类拦截 -> 包装 ApiResponse.error -> 返回
 * ResponseEntity。
 * Dependencies: Spring MVC 异常分派、BizException、ApiResponse。
 * Input: 控制器链路中冒泡出的 BizException。
 * Output: HTTP 200 响应体，内部携带业务错误码。
 * Boundary: 这里只处理业务异常；其他异常仍由 Spring 默认错误链路兜底。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException ex) {
        return ResponseEntity.ok(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }
}
