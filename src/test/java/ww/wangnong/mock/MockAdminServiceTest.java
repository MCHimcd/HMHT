package ww.wangnong.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ww.wangnong.entity.Node;
import ww.wangnong.entity.User;
import ww.wangnong.entity.UserNodeAccess;
import ww.wangnong.exception.BizException;
import ww.wangnong.repository.NodeRepository;
import ww.wangnong.repository.UserNodeAccessRepository;
import ww.wangnong.repository.UserRepository;
import ww.wangnong.service.AdminService.UserInfo;
import ww.wangnong.service.OperationLogService;

@ExtendWith(MockitoExtension.class)
class MockAdminServiceTest {

    @Mock
    private MockAuthService authService;
    @Mock
    private NodeRepository nodeRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private UserNodeAccessRepository userNodeAccessRepo;
    @Mock
    private OperationLogService opLog;

    private MockAdminService service;

    @BeforeEach
    void setUp() {
        service = new MockAdminService(authService, nodeRepo, userRepo,
                userNodeAccessRepo, opLog);
    }

    private User stubAdmin() {
        User u = new User();
        u.setId(1L);
        u.setUserName("管理员");
        u.setRole("admin");
        return u;
    }

    // ── createNode ──

    @Test
    void createNode_newNodeId_succeeds() {
        when(authService.requireAdmin("token")).thenReturn(stubAdmin());
        when(nodeRepo.findByNodeId("N01")).thenReturn(Optional.empty());

        service.createNode("token", "N01", "温室1号");

        verify(nodeRepo).save(any(Node.class));
    }

    @Test
    void createNode_duplicateNodeId_throwsBizException409() {
        when(authService.requireAdmin("token")).thenReturn(stubAdmin());
        when(nodeRepo.findByNodeId("N01")).thenReturn(Optional.of(new Node()));

        assertThatThrownBy(() -> service.createNode("token", "N01", "温室1号"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(409);
    }

    // ── deleteNode ──

    @Test
    void deleteNode_existingNode_deletesIt() {
        when(authService.requireAdmin("token")).thenReturn(stubAdmin());
        Node node = new Node();
        node.setNodeId("N01");
        node.setNodeName("温室1号");
        when(nodeRepo.findByNodeId("N01")).thenReturn(Optional.of(node));

        service.deleteNode("token", "N01");

        verify(nodeRepo).delete(node);
    }

    @Test
    void deleteNode_nonExistent_throwsBizException404() {
        when(authService.requireAdmin("token")).thenReturn(stubAdmin());
        when(nodeRepo.findByNodeId("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteNode("token", "NONEXISTENT"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(404);
    }

    // ── listUsers ──

    @Test
    void listUsers_returnsAllUsers() {
        when(authService.requireAdmin("token")).thenReturn(stubAdmin());
        User u = new User();
        u.setId(2L);
        u.setUserName("用户2");
        u.setPhone("13800000002");
        u.setRole("user");
        when(userRepo.findAll()).thenReturn(List.of(u));

        List<UserInfo> result = service.listUsers("token");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().userName()).isEqualTo("用户2");
    }

    // ── updateUserRole ──

    @Test
    void updateUserRole_existingUser_updatesRole() {
        when(authService.requireAdmin("token")).thenReturn(stubAdmin());
        User user = new User();
        user.setId(2L);
        user.setUserName("用户2");
        user.setRole("user");
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));

        service.updateUserRole("token", 2L, "admin");

        assertThat(user.getRole()).isEqualTo("admin");
        verify(userRepo).save(user);
    }

    // ── setUserNodeIds ──

    @Test
    void setUserNodeIds_replacesOldAccess() {
        when(authService.requireAdmin("token")).thenReturn(stubAdmin());

        service.setUserNodeIds("token", 2L, List.of("N01", "N02"));

        verify(userNodeAccessRepo).deleteByUserId(2L);
        verify(userNodeAccessRepo, times(2)).save(any(UserNodeAccess.class));
    }
}
