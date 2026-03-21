package ww.wangnong.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.VerificationCode;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone, LocalDateTime now);
}
