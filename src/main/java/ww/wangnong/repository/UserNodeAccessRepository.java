package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.UserNodeAccess;

/**
 * Purpose: 提供用户节点授权映射的查询与重写操作。
 * Responsibilities: 支持按用户或节点读取授权，并按用户维度删除旧授权后重建。
 * Flow: AdminService 授权重写；NodeService 权限校验时查询。
 * Dependencies: UserNodeAccess 实体。
 * Output: 授权映射列表或存在性判断。
 */
public interface UserNodeAccessRepository extends JpaRepository<UserNodeAccess, Long> {

    List<UserNodeAccess> findByUserId(Long userId);

    List<UserNodeAccess> findByNodeId(String nodeId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndNodeId(Long userId, String nodeId);

    boolean existsByUserIdAndNodeId(Long userId, String nodeId);
}
