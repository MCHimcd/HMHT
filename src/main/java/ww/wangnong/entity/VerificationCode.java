package ww.wangnong.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 保存短信验证码发放记录。
 * Responsibilities: 记录手机号、验证码、过期时间和使用状态，供注册/找回密码校验使用。
 * Flow: AuthService 发送验证码时写入 -> verifyCode 查询最近未使用且未过期记录 -> 成功后标记 used。
 * Dependencies: verification_codes 表、AuthService。
 * Input: 手机号、验证码、过期时间。
 * Output: 可校验的验证码记录。
 * Invariant: used=true 或 expires_at 过期后，该记录不应再次通过业务校验。
 */
@Entity
@Table(name = "verification_codes")
@Getter
@Setter
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
