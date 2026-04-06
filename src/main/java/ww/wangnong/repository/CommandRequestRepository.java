package ww.wangnong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CommandRequest;

public interface CommandRequestRepository extends JpaRepository<CommandRequest, Long> {

    Optional<CommandRequest> findByCommandId(String commandId);
}
