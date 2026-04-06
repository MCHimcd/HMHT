package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.CommandAck;

public interface CommandAckRepository extends JpaRepository<CommandAck, Long> {

    List<CommandAck> findByCommandIdOrderByCreatedAtDesc(String commandId);
}
