package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.UserNodeAccess;

public interface UserNodeAccessRepository extends JpaRepository<UserNodeAccess, Long> {

    List<UserNodeAccess> findByUserId(Long userId);

    List<UserNodeAccess> findByNodeId(String nodeId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndNodeId(Long userId, String nodeId);

    boolean existsByUserIdAndNodeId(Long userId, String nodeId);
}
