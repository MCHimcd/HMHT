package ww.wangnong.exception;

/**
 * Purpose: 表达可预期的业务失败，而不是基础设施异常。
 * Responsibilities: 携带业务状态码与消息，供全局异常处理器转换为 ApiResponse.error。
 * Flow: Service/Controller 抛出 BizException -> GlobalExceptionHandler 捕获 -> 返回
 * HTTP 200 + 业务 code。
 * Dependencies: GlobalExceptionHandler、ApiResponse。
 * Input: 业务错误码与错误消息。
 * Output: 供统一错误响应使用的运行时异常对象。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
