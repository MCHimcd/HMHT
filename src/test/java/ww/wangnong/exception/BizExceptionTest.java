package ww.wangnong.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BizExceptionTest {

    @Test
    void constructor_setsCodeAndMessage() {
        BizException ex = new BizException(401, "未登录");

        assertThat(ex.getCode()).isEqualTo(401);
        assertThat(ex.getMessage()).isEqualTo("未登录");
    }

    @Test
    void isRuntimeException() {
        BizException ex = new BizException(500, "error");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
