package ww.wangnong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByAlertId(String alertId);

    Page<Alert> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);

    Page<Alert> findByIsHandledOrderByCreatedAtDesc(boolean isHandled, Pageable pageable);

    List<Alert> findTop10ByIsHandledOrderByCreatedAtDesc(boolean isHandled);
}
