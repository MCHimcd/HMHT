package ww.wangnong.service;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface AuthService {

    record LoginResult(String token, @JsonProperty("expires_in") int expiresIn) {}

    record CodeResult(@JsonProperty("expires_in") int expiresIn, @JsonProperty("test_code") String testCode) {}

    record Profile(String userId, String userName, String phone, String gender,
                   String birthDate, String avatarSrc, String role) {}

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
}
