package ww.wangnong.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.VerificationCode;

/**
 * Purpose: 提供验证码校验查询。
 * Responsibilities: 按手机号读取最近一条未使用且未过期验证码。
 * Flow: AuthService 发码后写入；verifyCode 校验时读取最新有效记录。
 * Dependencies: VerificationCode 实体。
 * Output: 可用验证码记录。
 */
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    // Boundary: 校验逻辑依赖“最近一条有效验证码”语义，历史旧验证码即使未过期也不应优先返回。
    Optional<VerificationCode> findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone, LocalDateTime now);
}
