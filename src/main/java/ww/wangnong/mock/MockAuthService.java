package ww.wangnong.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ww.wangnong.entity.User;
import ww.wangnong.entity.VerificationCode;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.UserRepository;
import ww.wangnong.repository.VerificationCodeRepository;
import ww.wangnong.service.AuthService;
import ww.wangnong.service.OperationLogService;

@Service
public class MockAuthService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(MockAuthService.class);
    private static final Random RNG = new Random();

    private final UserRepository userRepo;
    private final VerificationCodeRepository codeRepo;
    private final OperationLogService opLog;
    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    public MockAuthService(UserRepository userRepo, VerificationCodeRepository codeRepo,
            OperationLogService opLog) {
        this.userRepo = userRepo;
        this.codeRepo = codeRepo;
        this.opLog = opLog;
    }

    @Override
    public LoginResult login(String username, String password) {
        User user = userRepo.findByPhone(username)
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        if (!user.getPasswordHash().equals(password)) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = UUID.randomUUID().toString();
        tokenToUserId.put(token, user.getId());
        opLog.log("LOGIN", user.getId(), user.getUserName(), "user", String.valueOf(user.getId()), "登录成功");
        return new LoginResult(token, 3600);
    }

    @Override
    public CodeResult sendVerificationCode(String phone) {
        String code = String.format("%04d", RNG.nextInt(10000));

        VerificationCode vc = new VerificationCode();
        vc.setPhone(phone);
        vc.setCode(code);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        codeRepo.save(vc);

        log.info("[MockAuth] 验证码 phone={}, code={}", phone, code);
        return new CodeResult(300, code);
    }

    @Override
    public void register(String phone, String verificationCode, String password) {
        verifyCode(phone, verificationCode);
        if (userRepo.existsByPhone(phone)) {
            throw new BizException(409, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(password);
        String suffix = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        user.setUserName("用户" + suffix);
        userRepo.save(user);
    }

    @Override
    public void forgotPassword(String phone, String verificationCode, String newPassword) {
        verifyCode(phone, verificationCode);
        User user = userRepo.findByPhone(phone)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        user.setPasswordHash(newPassword);
        userRepo.save(user);
    }

    @Override
    public Profile getProfile(String token) {
        return toProfile(requireUser(token));
    }

    @Override
    public Profile updateProfile(String token, String userName, String gender,
            String birthDate, String avatarSrc) {
        User user = requireUser(token);
        if (userName != null)
            user.setUserName(userName);
        if (gender != null)
            user.setGender(gender);
        if (birthDate != null)
            user.setBirthDate(LocalDate.parse(birthDate));
        if (avatarSrc != null)
            user.setAvatarSrc(avatarSrc);
        userRepo.save(user);
        return toProfile(user);
    }

    @Override
    public void changePassword(String token, String oldPassword, String newPassword) {
        User user = requireUser(token);
        if (!user.getPasswordHash().equals(oldPassword)) {
            throw new BizException(400, "旧密码错误");
        }
        user.setPasswordHash(newPassword);
        userRepo.save(user);
    }

    @Override
    public Profile useFixedAvatar(String token) {
        User user = requireUser(token);
        user.setAvatarSrc("https://example.com/fixed-avatar.png");
        userRepo.save(user);
        return toProfile(user);
    }

    @Override
    public void logout(String token) {
        Long userId = tokenToUserId.remove(token);
        if (userId != null) {
            opLog.log("LOGOUT", userId, null, "user", String.valueOf(userId), "登出");
        }
    }

    private void verifyCode(String phone, String code) {
        VerificationCode vc = codeRepo
                .findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        phone, LocalDateTime.now())
                .orElseThrow(() -> new BizException(400, "验证码错误或已过期"));
        if (!vc.getCode().equals(code)) {
            throw new BizException(400, "验证码错误");
        }
        vc.setUsed(true);
        codeRepo.save(vc);
    }

    public User requireUser(String token) {
        Long userId = tokenToUserId.get(token);
        if (userId == null)
            throw new BizException(401, "未登录或 token 已过期");
        return userRepo.findById(userId)
                .orElseThrow(() -> new BizException(401, "用户不存在"));
    }

    private Profile toProfile(User u) {
        return new Profile(
                "WangNong_" + String.format("%03d", u.getId()),
                u.getUserName(), u.getPhone(), u.getGender(),
                u.getBirthDate() != null ? u.getBirthDate().toString() : null,
                u.getAvatarSrc(), u.getRole());
    }

    public User requireAdmin(String token) {
        User user = requireUser(token);
        if (!"admin".equals(user.getRole())) {
            throw new BizException(403, "需要管理员权限");
        }
        return user;
    }
}
