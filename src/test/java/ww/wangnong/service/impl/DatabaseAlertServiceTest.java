package ww.wangnong.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import ww.wangnong.entity.Alert;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.AlertRepository;
import ww.wangnong.service.AlertService.AlertInfo;
import ww.wangnong.service.AlertService.AlertListResult;
import ww.wangnong.service.OperationLogService;

@ExtendWith(MockitoExtension.class)
class DatabaseAlertServiceTest {

    @Mock
    private AlertRepository alertRepo;
    @Mock
    private OperationLogService opLog;

    private DatabaseAlertService service;

    @BeforeEach
    void setUp() {
        service = new DatabaseAlertService(alertRepo, opLog);
    }

    private Alert stubAlert(String alertId, boolean handled) {
        Alert a = new Alert();
        a.setAlertId(alertId);
        a.setNodeId("N01");
        a.setType("temperature");
        a.setTypeName("温度异常");
        a.setLevel("warning");
        a.setMessage("温度超出阈值");
        a.setSource("sensor");
        a.setSourceName("温度传感器");
        a.setIsHandled(handled);
        a.setAlertTime(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }

    // ── addAlert ──

    @Test
    void addAlert_savesEntity() {
        when(alertRepo.count()).thenReturn(0L);

        AlertInfo info = new AlertInfo("ALT-001", "N01", "temperature", "温度异常",
                "warning", "温度超出", "sensor", "温度传感器", false, null,
                System.currentTimeMillis());

        service.addAlert(info);

        verify(alertRepo).save(any(Alert.class));
    }

    // ── queryAlerts ──

    @Test
    void queryAlerts_emptyQuery_returnsAll() {
        Alert a1 = stubAlert("ALT-001", false);
        Alert a2 = stubAlert("ALT-002", true);
        a2.setHandleTime(LocalDateTime.now());
        when(alertRepo.findAll(any(Sort.class))).thenReturn(List.of(a1, a2));

        AlertListResult result = service.queryAlerts(Map.of());

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.items()).hasSize(2);
        assertThat(result.unhandledCount()).isEqualTo(1);
    }

    @Test
    void queryAlerts_filterByStatus_returnsFiltered() {
        Alert a1 = stubAlert("ALT-001", false);
        Alert a2 = stubAlert("ALT-002", true);
        a2.setHandleTime(LocalDateTime.now());
        when(alertRepo.findAll(any(Sort.class))).thenReturn(List.of(a1, a2));

        AlertListResult result = service.queryAlerts(Map.of("filter_status", "unhandled"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.unhandledCount()).isEqualTo(1);
        assertThat(result.items()).allMatch(i -> !i.isHandled());
    }

    @Test
    void queryAlerts_handledFilter_keepsScopeUnhandledCount() {
        Alert a1 = stubAlert("ALT-001", false);
        Alert a2 = stubAlert("ALT-002", true);
        a2.setHandleTime(LocalDateTime.now());
        when(alertRepo.findAll(any(Sort.class))).thenReturn(List.of(a1, a2));

        AlertListResult result = service.queryAlerts(Map.of("filter_status", "handled"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.unhandledCount()).isEqualTo(1);
        assertThat(result.items()).allMatch(AlertInfo::isHandled);
    }

    // ── handleAlert ──

    @Test
    void handleAlert_existingAlert_marksHandled() {
        Alert alert = stubAlert("ALT-001", false);
        when(alertRepo.findByAlertId("ALT-001")).thenReturn(Optional.of(alert));

        service.handleAlert("ALT-001", "handle");

        assertThat(alert.getIsHandled()).isTrue();
        assertThat(alert.getHandleTime()).isNotNull();
        verify(alertRepo).save(alert);
    }

    @Test
    void handleAlert_nonExistent_throwsBizException404() {
        when(alertRepo.findByAlertId("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleAlert("NONEXISTENT", "handle"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(404);
    }

    // ── addAlert overflow ──

    @Test
    void addAlert_atMaxSize_deletesOldestThenSaves() {
        when(alertRepo.count()).thenReturn(200L);
        List<Alert> allAlerts = new ArrayList<>(
                IntStream.rangeClosed(1, 200)
                        .mapToObj(i -> stubAlert("ALT-" + i, false))
                        .toList());
        when(alertRepo.findAll(any(Sort.class))).thenReturn(allAlerts);

        AlertInfo info = new AlertInfo("ALT-NEW", "N01", "temperature", "温度异常",
                "warning", "新告警", "sensor", "温度传感器", false, null,
                System.currentTimeMillis());

        service.addAlert(info);

        // Should delete 1 oldest, then save new
        verify(alertRepo).delete(any(Alert.class));
        verify(alertRepo).save(any(Alert.class));
    }

    // ── queryAlerts startup scene ──

    @Test
    void queryAlerts_startupScene_returnsUnhandledPlusLimitedHandled() {
        List<Alert> alerts = new ArrayList<>();
        // 2 unhandled
        alerts.add(stubAlert("U1", false));
        alerts.add(stubAlert("U2", false));
        // 15 handled (only 10 should be included)
        for (int i = 1; i <= 15; i++) {
            Alert a = stubAlert("H" + i, true);
            a.setHandleTime(LocalDateTime.now());
            alerts.add(a);
        }
        when(alertRepo.findAll(any(Sort.class))).thenReturn(alerts);

        AlertListResult result = service.queryAlerts(Map.of("scene", "startup"));

        // 2 unhandled + 10 handled
        assertThat(result.total()).isEqualTo(12);
        assertThat(result.unhandledCount()).isEqualTo(2);
    }

    // ── queryAlerts filter_type ──

    @Test
    void queryAlerts_filterByType_filtersItems() {
        Alert a1 = stubAlert("ALT-001", false);
        a1.setType("temperature");
        Alert a2 = stubAlert("ALT-002", false);
        a2.setType("humidity");
        when(alertRepo.findAll(any(Sort.class))).thenReturn(List.of(a1, a2));

        AlertListResult result = service.queryAlerts(Map.of("filter_type", "temperature"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().getFirst().type()).isEqualTo("temperature");
    }

    @Test
    void queryAlerts_filterByNodeId_filtersItems() {
        Alert a1 = stubAlert("ALT-001", false);
        a1.setNodeId("N01");
        Alert a2 = stubAlert("ALT-002", false);
        a2.setNodeId("N02");
        when(alertRepo.findAll(any(Sort.class))).thenReturn(List.of(a1, a2));

        AlertListResult result = service.queryAlerts(Map.of("node_id", "N02"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().getFirst().nodeId()).isEqualTo("N02");
    }

    // ── queryAlerts pagination ──

    @Test
    void queryAlerts_pagination_returnsCorrectPage() {
        List<Alert> alerts = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> stubAlert("ALT-" + i, false)).toList();
        when(alertRepo.findAll(any(Sort.class))).thenReturn(alerts);

        AlertListResult page1 = service.queryAlerts(Map.of("page", 1, "pageSize", 2));
        AlertListResult page2 = service.queryAlerts(Map.of("page", 2, "pageSize", 2));
        AlertListResult page3 = service.queryAlerts(Map.of("page", 3, "pageSize", 2));

        assertThat(page1.items()).hasSize(2);
        assertThat(page2.items()).hasSize(2);
        assertThat(page3.items()).hasSize(1);
    }

    @Test
    void queryAlerts_pageOutOfRange_returnsEmptyList() {
        when(alertRepo.findAll(any(Sort.class))).thenReturn(List.of(stubAlert("A1", false)));

        AlertListResult result = service.queryAlerts(Map.of("page", 99, "pageSize", 20));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).isEmpty();
    }
}
