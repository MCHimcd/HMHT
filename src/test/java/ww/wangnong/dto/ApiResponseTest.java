package ww.wangnong.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void ok_withData_returns200AndData() {
        ApiResponse<String> resp = ApiResponse.ok("hello");

        assertThat(resp.code()).isEqualTo(200);
        assertThat(resp.message()).isEqualTo("success");
        assertThat(resp.data()).isEqualTo("hello");
    }

    @Test
    void ok_withoutData_returns200AndNullData() {
        ApiResponse<Object> resp = ApiResponse.ok();

        assertThat(resp.code()).isEqualTo(200);
        assertThat(resp.message()).isEqualTo("success");
        assertThat(resp.data()).isNull();
    }

    @Test
    void error_returnsCodeAndMessageWithNullData() {
        ApiResponse<Object> resp = ApiResponse.error(404, "not found");

        assertThat(resp.code()).isEqualTo(404);
        assertThat(resp.message()).isEqualTo("not found");
        assertThat(resp.data()).isNull();
    }
}
