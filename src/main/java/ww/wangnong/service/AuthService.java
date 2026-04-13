package ww.wangnong.service;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;

import ww.wangnong.entity.User;

/**
 * Purpose: 定义认证、资料与身份校验服务契约。
 * Responsibilities: 约束登录态、验证码、资料编辑、头像上传和管理员校验的统一入口。
 * Flow: Controller 提供 token 与表单参数 -> 实现类执行业务校验/持久化 -> 返回 record 结果或抛出
 * BizException。
 * Dependencies: User 实体、MultipartFile 上传抽象、实现类中的仓库与密码编码器。
 * Input: 用户名密码、验证码、token、资料字段和头像文件。
 * Output: 登录结果、资料快照，或已校验的用户实体。
 * Boundary: requireUser/requireAdmin 是其他服务共享的身份边界，不应绕过直接相信控制器输入。
 */
public interface AuthService {

        record LoginResult(String token, @JsonProperty("expires_in") int expiresIn) {
        }

        record CodeResult(@JsonProperty("expires_in") int expiresIn, @JsonProperty("test_code") String testCode) {
        }

        record Profile(String userId, String userName, String phone, String gender,
                        String birthDate, String avatarSrc, String role) {
        }

        LoginResult login(String username, String password);

        CodeResult sendVerificationCode(String phone);

        void register(String phone, String verificationCode, String password);

        void forgotPassword(String phone, String verificationCode, String newPassword);

        Profile getProfile(String token);

        Profile updateProfile(String token, String userName, String gender,
                        String birthDate, String avatarSrc);

        void changePassword(String token, String oldPassword, String newPassword);

        Profile useFixedAvatar(String token);

        Profile uploadAvatar(String token, MultipartFile file);

        void logout(String token);

        User requireUser(String token);

        User requireAdmin(String token);
}
