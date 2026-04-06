package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.entity.DeviceCapability;

@DataJpaTest
class DeviceCapabilityRepositoryTest {

    @Autowired
    private DeviceCapabilityRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void findTopByDeviceIdOrderByCreatedAtDesc_existingDevice_returnsLatest() {
        DeviceCapability oldCap = new DeviceCapability();
        oldCap.setDeviceId("D01");
        oldCap.setCapabilityVersion("1.0.0");
        oldCap.setChecksum("old");
        oldCap.setReportedAt(LocalDateTime.of(2026, 4, 6, 9, 0));
        em.persistAndFlush(oldCap);

        DeviceCapability newCap = new DeviceCapability();
        newCap.setDeviceId("D01");
        newCap.setCapabilityVersion("2.0.0");
        newCap.setChecksum("new");
        newCap.setReportedAt(LocalDateTime.of(2026, 4, 6, 10, 0));
        em.persistAndFlush(newCap);

        var found = repository.findTopByDeviceIdOrderByCreatedAtDesc("D01");
        assertThat(found).isPresent();
        assertThat(found.get().getCapabilityVersion()).isEqualTo("2.0.0");
    }

    @Test
    void findTopByDeviceIdOrderByCreatedAtDesc_unknownDevice_returnsEmpty() {
        assertThat(repository.findTopByDeviceIdOrderByCreatedAtDesc("D404")).isEmpty();
    }
}
