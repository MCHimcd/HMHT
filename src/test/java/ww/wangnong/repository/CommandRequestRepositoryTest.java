package ww.wangnong.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ww.wangnong.entity.CommandRequest;

@DataJpaTest
class CommandRequestRepositoryTest {

    @Autowired
    private CommandRequestRepository repo;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByCommandId_existingRequest_returnsRequest() {
        CommandRequest request = new CommandRequest();
        request.setCommandId("CMD-TEST-001");
        request.setDeviceId("A03_Node_01");
        request.setTargetKey("water_pump");
        request.setAction("turn_on");
        request.setStatus("sent");
        request.setSourceTopic("/wangnong_2026/node/A03_Node_01/command");
        request.setRelayTopic("/hmht/device/A03_Node_01/command");
        em.persistAndFlush(request);

        Optional<CommandRequest> found = repo.findByCommandId("CMD-TEST-001");

        assertThat(found).isPresent();
        assertThat(found.get().getDeviceId()).isEqualTo("A03_Node_01");
    }

    @Test
    void findByCommandId_nonExistent_returnsEmpty() {
        assertThat(repo.findByCommandId("CMD-NOT-EXIST")).isEmpty();
    }
}
