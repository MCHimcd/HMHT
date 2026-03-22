package ww.wangnong.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.NodeService;

@RestController
@RequestMapping("/v1/nodes")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping
    public ApiResponse<?> listNodes(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
        return ApiResponse.ok(nodeService.listNodes(extractToken(auth)));
    }

    @GetMapping("/{nodeId}")
    public ApiResponse<?> getNode(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable String nodeId) {
        return ApiResponse.ok(nodeService.getNode(extractToken(auth), nodeId));
    }

    private String extractToken(String auth) {
        return auth.startsWith("Bearer ") ? auth.substring(7) : auth;
    }

    @GetMapping("/{nodeId}/sensor-meta")
    public ApiResponse<?> getSensorMeta(@PathVariable String nodeId) {
        return ApiResponse.ok(nodeService.getSensorMeta(nodeId));
    }

    @GetMapping("/{nodeId}/equipment-meta")
    public ApiResponse<?> getEquipmentMeta(@PathVariable String nodeId) {
        return ApiResponse.ok(nodeService.getEquipmentMeta(nodeId));
    }
}
