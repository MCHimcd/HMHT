package ww.wangnong.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ww.wangnong.config.SecurityConfig;
import ww.wangnong.exception.BizException;
import ww.wangnong.service.AuthService;
import ww.wangnong.service.AuthService.CodeResult;
import ww.wangnong.service.AuthService.LoginResult;
import ww.wangnong.service.AuthService.Profile;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AuthService authService;

        @Test
        void login_success_returns200WithToken() throws Exception {
                when(authService.login("13800000001", "pass123"))
                                .thenReturn(new LoginResult("test-token", 3600));

                mockMvc.perform(post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"13800000001","password":"pass123"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.token").value("test-token"))
                                .andExpect(jsonPath("$.data.expires_in").value(3600));
        }

        @Test
        void login_wrongCredentials_returnsBizError() throws Exception {
                when(authService.login("13800000001", "wrong"))
                                .thenThrow(new BizException(401, "用户名或密码错误"));

                mockMvc.perform(post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"13800000001","password":"wrong"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        void sendVerificationCode_returnsCodeResult() throws Exception {
                when(authService.sendVerificationCode("13800000001"))
                                .thenReturn(new CodeResult(300, "1234"));

                mockMvc.perform(post("/v1/auth/send-verification-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"phone":"13800000001"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.expires_in").value(300));
        }

        @Test
        void getProfile_validToken_returnsProfile() throws Exception {
                when(authService.getProfile("test-token"))
                                .thenReturn(new Profile("WangNong_001", "测试用户", "13800000001",
                                                null, null, null, "user"));

                mockMvc.perform(get("/v1/auth/profile")
                                .header("Authorization", "Bearer test-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.userName").value("测试用户"));
        }

        @Test
        void getProfile_invalidToken_returnsBizError() throws Exception {
                when(authService.getProfile("invalid"))
                                .thenThrow(new BizException(401, "未登录或 token 已过期"));

                mockMvc.perform(get("/v1/auth/profile")
                                .header("Authorization", "Bearer invalid"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        void register_duplicatePhone_returns409() throws Exception {
                doThrow(new BizException(409, "手机号已注册"))
                                .when(authService).register(anyString(), anyString(), anyString());

                mockMvc.perform(post("/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"phone":"13800000001","verificationCode":"1234","password":"pass123"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(409))
                                .andExpect(jsonPath("$.message").value("手机号已注册"));
        }

        @Test
        void forgotPassword_phoneNotFound_returns404() throws Exception {
                doThrow(new BizException(404, "手机号未注册"))
                                .when(authService).forgotPassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/v1/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"phone":"13899999999","verificationCode":"1234","newPassword":"new123"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(404))
                                .andExpect(jsonPath("$.message").value("手机号未注册"));
        }

        @Test
        void changePassword_wrongOldPassword_returns400() throws Exception {
                doThrow(new BizException(400, "原密码错误"))
                                .when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(put("/v1/auth/password")
                                .header("Authorization", "Bearer test-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"oldPassword":"wrong","newPassword":"new123"}"""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(400))
                                .andExpect(jsonPath("$.message").value("原密码错误"));
        }
}
