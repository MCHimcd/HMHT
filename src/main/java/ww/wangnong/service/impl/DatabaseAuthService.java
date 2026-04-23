package ww.wangnong.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ww.wangnong.entity.User;
import ww.wangnong.entity.VerificationCode;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.UserRepository;
import ww.wangnong.repository.VerificationCodeRepository;
import ww.wangnong.service.AuthService;
import ww.wangnong.service.OperationLogService;

/**
 * Purpose: AuthService 的数据库实现。
 * Responsibilities: 处理登录、验证码、注册、资料修改和基于内存 token 的身份校验。
 * Flow: 控制器入参 -> 仓库查询/密码校验 -> 维护 tokenStore 或用户资料 -> 写入操作日志 -> 返回 AuthService
 * record。
 * Dependencies:
 * UserRepository、VerificationCodeRepository、PasswordEncoder、OperationLogService。
 * Input: 用户名密码、验证码、token、资料字段、头像文件。
 * Output: 登录结果、资料快照或用户实体。
 * Side Effects: 写 verification_codes/users、维护内存 tokenStore、输出审计日志。
 * Boundary: tokenStore 只存在于当前服务实例内，重启后全部登录态失效。
 */
@Service
public class DatabaseAuthService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAuthService.class);
    private static final Random RNG = new Random();
    private static final long TOKEN_TTL_SECONDS = 3600;

    private final UserRepository userRepo;
    private final VerificationCodeRepository codeRepo;
    private final OperationLogService opLog;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();

    public DatabaseAuthService(UserRepository userRepo, VerificationCodeRepository codeRepo,
            OperationLogService opLog, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.codeRepo = codeRepo;
        this.opLog = opLog;
        this.passwordEncoder = passwordEncoder;
    }

    private record TokenEntry(Long userId, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Override
    public LoginResult login(String username, String password) {
        User user = userRepo.findByPhone(username)
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, new TokenEntry(user.getId(), Instant.now().plusSeconds(TOKEN_TTL_SECONDS)));
        opLog.log("LOGIN", user.getId(), user.getUserName(), "user", String.valueOf(user.getId()), "登录成功");
        return new LoginResult(token, (int) TOKEN_TTL_SECONDS);
    }

    @Override
    public CodeResult sendVerificationCode(String phone) {
        String code = String.format("%04d", RNG.nextInt(10000));

        VerificationCode vc = new VerificationCode();
        vc.setPhone(phone);
        vc.setCode(code);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        codeRepo.save(vc);

        log.info("[Auth] 验证码 phone={}, code={}", phone, code);
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
        user.setPasswordHash(passwordEncoder.encode(password));
        String suffix = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        user.setUserName("用户" + suffix);
        userRepo.save(user);
    }

    @Override
    public void forgotPassword(String phone, String verificationCode, String newPassword) {
        verifyCode(phone, verificationCode);
        User user = userRepo.findByPhone(phone)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
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
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BizException(400, "旧密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    @Override
    public void logout(String token) {
        TokenEntry entry = tokenStore.remove(token);
        if (entry != null) {
            opLog.log("LOGOUT", entry.userId(), null, "user", String.valueOf(entry.userId()), "登出");
        }
    }

    private void verifyCode(String phone, String code) {
        VerificationCode vc = codeRepo
                .findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        phone, LocalDateTime.now())
                .orElseThrow(() -> new BizException(400, "验证码错误或已过期"));
        // Boundary: 同手机号只校验最近一条未使用且未过期验证码，旧码即使正确也不再接受。
        if (!vc.getCode().equals(code)) {
            throw new BizException(400, "验证码错误");
        }
        vc.setUsed(true);
        codeRepo.save(vc);
    }

    @Override
    public User requireUser(String token) {
        TokenEntry entry = tokenStore.get(token);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                tokenStore.remove(token);
            }
            throw new BizException(401, "未登录或 token 已过期");
        }
        return userRepo.findById(entry.userId())
                .orElseThrow(() -> new BizException(401, "用户不存在"));
    }

    /** 定期清理过期 token，防止内存泄漏。 */
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    void evictExpiredTokens() {
        Iterator<Map.Entry<String, TokenEntry>> it = tokenStore.entrySet().iterator();
        int evicted = 0;
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
                evicted++;
            }
        }
        if (evicted > 0) {
            log.debug("[Auth] Evicted {} expired tokens, remaining {}", evicted, tokenStore.size());
        }
    }

    private Profile toProfile(User u) {
        return new Profile(
                "WangNong_" + String.format("%03d", u.getId()),
                u.getUserName(), u.getPhone(), u.getGender(),
                u.getBirthDate() != null ? u.getBirthDate().toString() : null,
                u.getAvatarSrc(), u.getRole());
    }

    @Override
    public User requireAdmin(String token) {
        User user = requireUser(token);
        if (!"admin".equals(user.getRole())) {
            throw new BizException(403, "需要管理员权限");
        }
        return user;
    }
}
