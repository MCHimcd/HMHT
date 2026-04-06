package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.entity.DeviceShadow;

@DataJpaTest
class DeviceShadowRepositoryTest {

    @Autowired
    private DeviceShadowRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByDeviceId_existingShadow_returnsShadow() {
        DeviceShadow shadow = new DeviceShadow();
        shadow.setDeviceId("D01");
        shadow.setShadowJson("{\"pump\":\"off\"}");
        em.persistAndFlush(shadow);

        var found = repository.findByDeviceId("D01");
        assertThat(found).isPresent();
        assertThat(found.get().getShadowJson()).contains("pump");
    }

    @Test
    void findByDeviceId_missingShadow_returnsEmpty() {
        assertThat(repository.findByDeviceId("D404")).isEmpty();
    }
}
