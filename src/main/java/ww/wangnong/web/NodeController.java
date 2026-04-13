package ww.wangnong.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ww.wangnong.dto.ApiResponse;
import ww.wangnong.service.NodeService;

/**
 * Purpose: 暴露节点列表、详情、元数据和阈值管理接口。
 * Responsibilities: 从 Authorization 头提取业务 token，转发节点查询与阈值更新请求。
 * Flow: HTTP 请求 -> 提取 token/path/body -> NodeService 权限校验与聚合查询 ->
 * ApiResponse.ok。
 * Dependencies: NodeService、ApiResponse。
 * Input: Bearer token、节点标识、阈值更新列表。
 * Output: 节点详情、节点元数据或空成功响应。
 * Side Effects: 阈值更新会修改节点传感器配置。
 */
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

    @PutMapping("/{nodeId}/sensor-thresholds")
    public ApiResponse<?> updateThresholds(
            @RequestHeader(value = "Authorization", defaultValue = "") String auth,
            @PathVariable String nodeId,
            @RequestBody List<NodeService.ThresholdUpdate> thresholds) {
        nodeService.updateSensorThresholds(extractToken(auth), nodeId, thresholds);
        return ApiResponse.ok();
    }
}
