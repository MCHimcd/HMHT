package ww.wangnong.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ww.wangnong.dto.ApiResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBizException_returnsOkWithErrorCode() {
        BizException ex = new BizException(401, "未登录或 token 已过期");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBizException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("未登录或 token 已过期");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void handleBizException_403_returnsCorrectCode() {
        BizException ex = new BizException(403, "需要管理员权限");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBizException(ex);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("需要管理员权限");
    }
}
