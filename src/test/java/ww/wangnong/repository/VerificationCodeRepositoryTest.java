package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.TestDataFactory;
import ww.wangnong.entity.VerificationCode;

@DataJpaTest
class VerificationCodeRepositoryTest {

    @Autowired
    private VerificationCodeRepository repo;

    @Autowired
    private TestEntityManager em;

    @Test
    void findFirstByPhoneAndUsedFalseAndExpiresAtAfter_validCode_returnsCode() {
        VerificationCode vc = TestDataFactory.verificationCode("13800000001", "1234");
        em.persistAndFlush(vc);

        Optional<VerificationCode> found = repo
                .findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        "13800000001", LocalDateTime.now());

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("1234");
    }

    @Test
    void findFirstByPhoneAndUsedFalseAndExpiresAtAfter_usedCode_returnsEmpty() {
        VerificationCode vc = TestDataFactory.verificationCode("13800000001", "1234");
        vc.setUsed(true);
        em.persistAndFlush(vc);

        Optional<VerificationCode> found = repo
                .findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        "13800000001", LocalDateTime.now());

        assertThat(found).isEmpty();
    }

    @Test
    void findFirstByPhoneAndUsedFalseAndExpiresAtAfter_expiredCode_returnsEmpty() {
        VerificationCode vc = TestDataFactory.verificationCode("13800000001", "1234");
        vc.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        em.persistAndFlush(vc);

        Optional<VerificationCode> found = repo
                .findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        "13800000001", LocalDateTime.now());

        assertThat(found).isEmpty();
    }

    @Test
    void findFirstByPhoneAndUsedFalseAndExpiresAtAfter_wrongPhone_returnsEmpty() {
        VerificationCode vc = TestDataFactory.verificationCode("13800000001", "1234");
        em.persistAndFlush(vc);

        Optional<VerificationCode> found = repo
                .findFirstByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        "13899999999", LocalDateTime.now());

        assertThat(found).isEmpty();
    }
}
