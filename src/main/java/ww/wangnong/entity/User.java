package ww.wangnong.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * Purpose: 保存系统用户主档案。
 * Responsibilities: 记录手机号、密码哈希、昵称、性别、生日、头像和角色信息，支撑认证与授权。
 * Flow: 注册/种子流程创建用户 -> AuthService 校验并更新资料 -> 其他服务按 role/id 做权限判断。
 * Dependencies: users 表、AuthService。
 * Input: 用户注册与资料更新字段。
 * Output: 用户主记录。
 * Boundary: passwordHash 存储的是编码后密码，业务层不应写入明文。
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "avatar_src", length = 500)
    private String avatarSrc;

    @Column(nullable = false, length = 20)
    private String role = "user";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
