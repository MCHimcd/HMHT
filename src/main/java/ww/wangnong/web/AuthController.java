package ww.wangnong.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.AuthService;

/**
 * Purpose: 暴露认证与用户资料相关 REST 端点。
 * Responsibilities: 接收登录、注册、验证码、密码、头像和资料请求；提取 Bearer token；将请求委托给 AuthService。
 * Flow: HTTP 请求 -> 解析 request record 或 multipart -> extractToken -> AuthService
 * -> ApiResponse.ok。
 * Dependencies: AuthService、ApiResponse、Spring Multipart 支持。
 * Input: JSON 请求体、Authorization 请求头、头像上传文件。
 * Output: 登录结果、用户资料或空成功响应。
 * Side Effects: 可能触发验证码发送、密码修改、头像文件写入与操作日志记录。
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req.username(), req.password()));
    }

    record SendCodeRequest(String phone) {
    }

    @PostMapping("/send-verification-code")
    public ApiResponse<?> sendCode(@RequestBody SendCodeRequest req) {
        return ApiResponse.ok(authService.sendVerificationCode(req.phone()));
    }

    record RegisterRequest(String phone, String verificationCode, String password) {
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest req) {
        authService.register(req.phone(), req.verificationCode(), req.password());
        return ApiResponse.ok();
    }

    record ForgotPasswordRequest(String phone, String verificationCode, String newPassword) {
    }

    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.phone(), req.verificationCode(), req.newPassword());
        return ApiResponse.ok();
    }

    @GetMapping("/profile")
    public ApiResponse<?> getProfile(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
        return ApiResponse.ok(authService.getProfile(extractToken(auth)));
    }

    record UpdateProfileRequest(String userName, String gender, String birthDate, String avatarSrc) {
    }

    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @RequestBody UpdateProfileRequest req) {
        return ApiResponse.ok(authService.updateProfile(
                extractToken(auth), req.userName(), req.gender(), req.birthDate(), req.avatarSrc()));
    }

    record ChangePasswordRequest(String oldPassword, String newPassword) {
    }

    @PutMapping("/password")
    public ApiResponse<?> changePassword(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @RequestBody ChangePasswordRequest req) {
        authService.changePassword(extractToken(auth), req.oldPassword(), req.newPassword());
        return ApiResponse.ok();
    }

    @PostMapping("/avatar/use-fixed")
    public ApiResponse<?> useFixedAvatar(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
        return ApiResponse.ok(authService.useFixedAvatar(extractToken(auth)));
    }

    @PostMapping("/avatar/upload")
    public ApiResponse<?> uploadAvatar(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(authService.uploadAvatar(extractToken(auth), file));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
        authService.logout(extractToken(auth));
        return ApiResponse.ok();
    }

    private String extractToken(String auth) {
        return auth.startsWith("Bearer ") ? auth.substring(7) : auth;
    }
}
