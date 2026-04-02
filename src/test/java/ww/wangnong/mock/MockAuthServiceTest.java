package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.entity.User;
import ww.wangnong.entity.VerificationCode;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.UserRepository;
import ww.wangnong.repository.VerificationCodeRepository;
import ww.wangnong.service.AuthService.CodeResult;
import ww.wangnong.service.AuthService.LoginResult;
import ww.wangnong.service.AuthService.Profile;
import ww.wangnong.service.OperationLogService;

@ExtendWith(MockitoExtension.class)
class MockAuthServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private VerificationCodeRepository codeRepo;
    @Mock
    private OperationLogService opLog;

    private MockAuthService service;

    @BeforeEach
    void setUp() {
        service = new MockAuthService(userRepo, codeRepo, opLog);
    }

    private User stubUser() {
        User u = new User();
        u.setId(1L);
        u.setPhone("13800000001");
        u.setPasswordHash("password123");
        u.setUserName("测试用户");
        u.setRole("user");
        return u;
    }

    // ── login ──

    @Test
    void login_validCredentials_returnsToken() {
        User user = stubUser();
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(user));

        LoginResult result = service.login("13800000001", "password123");

        assertThat(result.token()).isNotBlank();
        assertThat(result.expiresIn()).isEqualTo(3600);
    }

    @Test
    void login_wrongPassword_throwsBizException401() {
        User user = stubUser();
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login("13800000001", "wrong"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(401);
    }

    @Test
    void login_nonExistentUser_throwsBizException401() {
        when(userRepo.findByPhone("99999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("99999999999", "any"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(401);
    }

    // ── sendVerificationCode ──

    @Test
    void sendVerificationCode_savesCodeAndReturns() {
        CodeResult result = service.sendVerificationCode("13800000001");

        assertThat(result.expiresIn()).isEqualTo(300);
        assertThat(result.testCode()).hasSize(4);
        verify(codeRepo).save(any(VerificationCode.class));
    }

    // ── register ──

    @Test
    void register_newUser_succeeds() {
        VerificationCode vc = new VerificationCode();
        vc.setCode("1234");
        vc.setUsed(false);
        when(codeRepo.findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                anyString(), any(LocalDateTime.class))).thenReturn(Optional.of(vc));
        when(userRepo.existsByPhone("13800000001")).thenReturn(false);

        service.register("13800000001", "1234", "newpass");

        verify(userRepo).save(any(User.class));
    }

    @Test
    void register_existingPhone_throwsBizException409() {
        VerificationCode vc = new VerificationCode();
        vc.setCode("1234");
        vc.setUsed(false);
        when(codeRepo.findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                anyString(), any(LocalDateTime.class))).thenReturn(Optional.of(vc));
        when(userRepo.existsByPhone("13800000001")).thenReturn(true);

        assertThatThrownBy(() -> service.register("13800000001", "1234", "pass"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(409);
    }

    // ── getProfile ──

    @Test
    void getProfile_validToken_returnsProfile() {
        User user = stubUser();
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(user));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        LoginResult login = service.login("13800000001", "password123");
        Profile profile = service.getProfile(login.token());

        assertThat(profile.userName()).isEqualTo("测试用户");
        assertThat(profile.phone()).isEqualTo("13800000001");
    }

    @Test
    void getProfile_invalidToken_throwsBizException401() {
        assertThatThrownBy(() -> service.getProfile("invalid-token"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(401);
    }

    // ── changePassword ──

    @Test
    void changePassword_wrongOldPassword_throwsBizException400() {
        User user = stubUser();
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(user));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        LoginResult login = service.login("13800000001", "password123");

        assertThatThrownBy(() -> service.changePassword(login.token(), "wrongOld", "newPass"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(400);
    }

    // ── requireAdmin ──

    @Test
    void requireAdmin_nonAdmin_throwsBizException403() {
        User user = stubUser();
        user.setRole("user");
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(user));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        LoginResult login = service.login("13800000001", "password123");

        assertThatThrownBy(() -> service.requireAdmin(login.token()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(403);
    }

    @Test
    void requireAdmin_admin_returnsUser() {
        User user = stubUser();
        user.setRole("admin");
        when(userRepo.findByPhone("13800000001")).thenReturn(Optional.of(user));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        LoginResult login = service.login("13800000001", "password123");
        User admin = service.requireAdmin(login.token());

        assertThat(admin.getRole()).isEqualTo("admin");
    }
}
