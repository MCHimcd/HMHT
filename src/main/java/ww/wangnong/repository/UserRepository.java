package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.User;

/**
 * Purpose: 提供用户主档案查询。
 * Responsibilities: 按手机号读取用户并判断手机号是否已注册。
 * Flow: AuthService、AdminService 和种子流程读写用户。
 * Dependencies: User 实体。
 * Output: 用户记录或唯一性判断。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
