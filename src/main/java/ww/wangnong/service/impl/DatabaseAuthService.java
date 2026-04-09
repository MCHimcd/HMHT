package ww.wangnong.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ww.wangnong.entity.User;
import ww.wangnong.entity.VerificationCode;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.UserRepository;
import ww.wangnong.repository.VerificationCodeRepository;
import ww.wangnong.service.AuthService;
import ww.wangnong.service.OperationLogService;

@Service
public class DatabaseAuthService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAuthService.class);
    private static final Random RNG = new Random();
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final UserRepository userRepo;
    private final VerificationCodeRepository codeRepo;
    private final OperationLogService opLog;
    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    @Value("${hmht.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Value("${server.port:8080}")
    private int serverPort;

    public DatabaseAuthService(UserRepository userRepo, VerificationCodeRepository codeRepo,
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
    public Profile uploadAvatar(String token, MultipartFile file) {
        User user = requireUser(token);

        if (file == null || file.isEmpty()) {
            throw new BizException(400, "文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BizException(400, "仅支持 PNG/JPEG 格式图片");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(400, "文件大小不能超过 5MB");
        }

        String extension = contentType.contains("png") ? "png" : "jpg";
        String filename = String.format("user_%d_%d.%s", user.getId(), System.currentTimeMillis(), extension);
        Path avatarRoot = resolveAvatarRootDir();
        Path targetPath = avatarRoot.resolve(filename);

        try {
            Files.createDirectories(avatarRoot);
            file.transferTo(targetPath.toFile());
            log.info("[Auth] 头像已保存: {}", targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[Auth] 保存头像失败", e);
            throw new BizException(500, "保存头像失败");
        }

        deleteOldAvatar(user);

        String avatarUrl = String.format("http://localhost:%d/api/uploads/avatars/%s", serverPort, filename);
        user.setAvatarSrc(avatarUrl);
        userRepo.save(user);

        opLog.log("AVATAR_UPLOAD", user.getId(), user.getUserName(), "user", String.valueOf(user.getId()), "上传头像");
        return toProfile(user);
    }

    private void deleteOldAvatar(User user) {
        String oldAvatarSrc = user.getAvatarSrc();
        if (oldAvatarSrc != null && oldAvatarSrc.contains("/uploads/avatars/")) {
            String oldFilename = oldAvatarSrc.substring(oldAvatarSrc.lastIndexOf('/') + 1);
            Path oldPath = resolveAvatarRootDir().resolve(oldFilename);
            try {
                Files.deleteIfExists(oldPath);
                log.info("[Auth] 已删除旧头像: {}", oldPath);
            } catch (IOException e) {
                log.warn("[Auth] 删除旧头像失败: {}", oldPath, e);
            }
        }
    }

    private Path resolveAvatarRootDir() {
        String configuredDir = avatarDir;
        if (configuredDir == null || configuredDir.isBlank()) {
            configuredDir = "uploads/avatars";
        }
        return Paths.get(configuredDir).toAbsolutePath().normalize();
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

    @Override
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

    @Override
    public User requireAdmin(String token) {
        User user = requireUser(token);
        if (!"admin".equals(user.getRole())) {
            throw new BizException(403, "需要管理员权限");
        }
        return user;
    }
}
