package ww.wangnong.service;

import java.util.List;

public interface AdminService {

    record UserInfo(long id, String userName, String phone, String role) {}

    void createNode(String token, String nodeId, String nodeName);

    void updateNode(String token, String nodeId, String nodeName);

    void deleteNode(String token, String nodeId);

    List<UserInfo> listUsers(String token);

    void updateUserRole(String token, long userId, String role);
}
