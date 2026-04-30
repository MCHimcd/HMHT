import { request } from "./client";
import type { EquipmentMeta, NodeDetail, NodeInfo, SensorMeta, ThresholdUpdate } from "./types";

export function listNodes() {
  return request<NodeInfo[]>("/v1/nodes");
}

export function getNode(nodeId: string) {
  return request<NodeDetail>(`/v1/nodes/${encodeURIComponent(nodeId)}`);
}

export function getSensorMeta(nodeId: string) {
  return request<SensorMeta[]>(`/v1/nodes/${encodeURIComponent(nodeId)}/sensor-meta`);
}

export function getEquipmentMeta(nodeId: string) {
  return request<EquipmentMeta[]>(`/v1/nodes/${encodeURIComponent(nodeId)}/equipment-meta`);
}

export function updateThresholds(nodeId: string, thresholds: ThresholdUpdate[]) {
  return request<null>(`/v1/nodes/${encodeURIComponent(nodeId)}/sensor-thresholds`, {
    method: "PUT",
    body: thresholds,
  });
}
