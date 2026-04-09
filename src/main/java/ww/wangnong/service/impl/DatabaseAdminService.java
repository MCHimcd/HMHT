package ww.wangnong.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import ww.wangnong.entity.Node;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.UserNodeAccessRepository;
import ww.wangnong.repository.UserRepository;
import ww.wangnong.service.AdminService;
import ww.wangnong.service.AuthService;
import ww.wangnong.service.OperationLogService;

@Service
public class DatabaseAdminService implements AdminService {

    private final AuthService authService;
    private final NodeRepository nodeRepo;
    private final UserRepository userRepo;
    private final UserNodeAccessRepository userNodeAccessRepo;
    private final OperationLogService opLog;

    public DatabaseAdminService(AuthService authService,
            NodeRepository nodeRepo,
            UserRepository userRepo,
            UserNodeAccessRepository userNodeAccessRepo,
            OperationLogService opLog) {
        this.authService = authService;
        this.nodeRepo = nodeRepo;
        this.userRepo = userRepo;
        this.userNodeAccessRepo = userNodeAccessRepo;
        this.opLog = opLog;
    }

    @Override
    public void createNode(String token, String nodeId, String nodeName) {
        User admin = authService.requireAdmin(token);
        if (nodeRepo.findByNodeId(nodeId).isPresent()) {
            throw new BizException(409, "节点已存在");
        }
        Node node = new Node();
        node.setNodeId(nodeId);
        node.setNodeName(nodeName);
        node.setUser(admin);
        node.setOnline(false);
        node.setSmartMode(false);
        nodeRepo.save(node);
        opLog.log("NODE_CREATE", admin.getId(), admin.getUserName(), "node", nodeId, nodeName);
    }

    @Override
    public void updateNode(String token, String nodeId, String nodeName) {
        User admin = authService.requireAdmin(token);
        Node node = nodeRepo.findByNodeId(nodeId)
                .orElseThrow(() -> new BizException(404, "节点不存在"));
        if (nodeName != null)
            node.setNodeName(nodeName);
        nodeRepo.save(node);
        opLog.log("NODE_UPDATE", admin.getId(), admin.getUserName(), "node", nodeId, nodeName);
    }

    @Override
    public void deleteNode(String token, String nodeId) {
        User admin = authService.requireAdmin(token);
        Node node = nodeRepo.findByNodeId(nodeId)
                .orElseThrow(() -> new BizException(404, "节点不存在"));
        nodeRepo.delete(node);
        opLog.log("NODE_DELETE", admin.getId(), admin.getUserName(), "node", nodeId, node.getNodeName());
    }

    @Override
    public List<UserInfo> listUsers(String token) {
        authService.requireAdmin(token);
        return userRepo.findAll().stream()
                .map(u -> new UserInfo(u.getId(), u.getUserName(), u.getPhone(), u.getRole()))
                .toList();
    }

    @Override
    public void updateUserRole(String token, long userId, String role) {
        User admin = authService.requireAdmin(token);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        String oldRole = user.getRole();
        user.setRole(role);
        userRepo.save(user);
        opLog.log("ROLE_CHANGE", admin.getId(), admin.getUserName(), "user", String.valueOf(userId),
                user.getUserName() + " " + oldRole + " → " + role);
    }

    @Override
    public List<String> getUserNodeIds(String token, long userId) {
        authService.requireAdmin(token);
        return userNodeAccessRepo.findByUserId(userId).stream()
                .map(UserNodeAccess::getNodeId).toList();
    }

    @Override
    public void setUserNodeIds(String token, long userId, List<String> nodeIds) {
        User admin = authService.requireAdmin(token);
        userNodeAccessRepo.deleteByUserId(userId);
        for (String nodeId : nodeIds) {
            UserNodeAccess access = new UserNodeAccess();
            access.setUserId(userId);
            access.setNodeId(nodeId);
            userNodeAccessRepo.save(access);
        }
        opLog.log("NODE_UPDATE", admin.getId(), admin.getUserName(), "user_node_access",
                String.valueOf(userId), "授权节点: " + String.join(",", nodeIds));
    }
}
