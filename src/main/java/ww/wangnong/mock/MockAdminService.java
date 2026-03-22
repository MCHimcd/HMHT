package ww.wangnong.mock;

import java.util.List;

import org.springframework.stereotype.Service;

import ww.wangnong.entity.Node;
import ww.wangnong.entity.User;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.UserRepository;
import ww.wangnong.service.AdminService;

@Service
public class MockAdminService implements AdminService {

    private final MockAuthService authService;
    private final NodeRepository nodeRepo;
    private final UserRepository userRepo;

    public MockAdminService(MockAuthService authService,
                            NodeRepository nodeRepo,
                            UserRepository userRepo) {
        this.authService = authService;
        this.nodeRepo = nodeRepo;
        this.userRepo = userRepo;
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
    }

    @Override
    public void updateNode(String token, String nodeId, String nodeName) {
        authService.requireAdmin(token);
        Node node = nodeRepo.findByNodeId(nodeId)
                .orElseThrow(() -> new BizException(404, "节点不存在"));
        if (nodeName != null) node.setNodeName(nodeName);
        nodeRepo.save(node);
    }

    @Override
    public void deleteNode(String token, String nodeId) {
        authService.requireAdmin(token);
        Node node = nodeRepo.findByNodeId(nodeId)
                .orElseThrow(() -> new BizException(404, "节点不存在"));
        nodeRepo.delete(node);
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
        authService.requireAdmin(token);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        user.setRole(role);
        userRepo.save(user);
    }
}
