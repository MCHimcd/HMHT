package ww.wangnong.service;

import java.util.List;

/**
 * Purpose: 定义管理员节点与授权管理能力。
 * Responsibilities: 管理节点生命周期、用户角色和节点授权映射。
 * Flow: 控制器传入 token 与管理指令 -> 实现类完成管理员校验、仓库写入与审计记录。
 * Dependencies: 节点、用户、用户节点授权相关持久化模型。
 * Input: 管理员 token、节点标识、用户标识、角色和授权节点列表。
 * Output: 用户列表、授权节点列表或空成功结果。
 */
public interface AdminService {

    record UserInfo(long id, String userName, String phone, String role) {
    }

    void createNode(String token, String nodeId, String nodeName);

    void updateNode(String token, String nodeId, String nodeName);

    void deleteNode(String token, String nodeId);

    List<UserInfo> listUsers(String token);

    void updateUserRole(String token, long userId, String role);

    List<String> getUserNodeIds(String token, long userId);

    void setUserNodeIds(String token, long userId, List<String> nodeIds);
}
