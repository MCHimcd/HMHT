package ww.wangnong.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.AdminService;

@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    record CreateNodeRequest(String nodeId, String nodeName) {
    }

    @PostMapping("/nodes")
    public ApiResponse<?> createNode(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @RequestBody CreateNodeRequest req) {
        adminService.createNode(extractToken(auth), req.nodeId(), req.nodeName());
        return ApiResponse.ok();
    }

    record UpdateNodeRequest(String nodeName) {
    }

    @PutMapping("/nodes/{nodeId}")
    public ApiResponse<?> updateNode(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable String nodeId,
            @RequestBody UpdateNodeRequest req) {
        adminService.updateNode(extractToken(auth), nodeId, req.nodeName());
        return ApiResponse.ok();
    }

    @DeleteMapping("/nodes/{nodeId}")
    public ApiResponse<?> deleteNode(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable String nodeId) {
        adminService.deleteNode(extractToken(auth), nodeId);
        return ApiResponse.ok();
    }

    @GetMapping("/users")
    public ApiResponse<?> listUsers(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
        return ApiResponse.ok(adminService.listUsers(extractToken(auth)));
    }

    @PutMapping("/users/{userId}/role")
    public ApiResponse<?> updateUserRole(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable long userId,
            @RequestBody Map<String, String> body) {
        adminService.updateUserRole(extractToken(auth), userId, body.get("role"));
        return ApiResponse.ok();
    }

    @GetMapping("/users/{userId}/nodes")
    public ApiResponse<?> getUserNodes(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable long userId) {
        return ApiResponse.ok(adminService.getUserNodeIds(extractToken(auth), userId));
    }

    record SetUserNodesRequest(List<String> nodeIds) {
    }

    @PutMapping("/users/{userId}/nodes")
    public ApiResponse<?> setUserNodes(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable long userId,
            @RequestBody SetUserNodesRequest req) {
        adminService.setUserNodeIds(extractToken(auth), userId, req.nodeIds());
        return ApiResponse.ok();
    }

    private String extractToken(String auth) {
        return auth.startsWith("Bearer ") ? auth.substring(7) : auth;
    }
}
