package ww.wangnong.dto;

/**
 * Purpose: 统一 HMHT REST 响应包裹结构。
 * Responsibilities: 约束成功与失败响应都使用 code/message/data 三段式返回，减少控制器重复拼装。
 * Flow: Controller 或异常处理器调用 ok/error 工厂方法 -> 生成不可变响应 record -> 交给 Spring 序列化。
 * Dependencies: Controller、GlobalExceptionHandler。
 * Input: 业务返回数据或错误码/错误消息。
 * Output: 面向前端的标准 JSON 响应模型。
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "success", null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
