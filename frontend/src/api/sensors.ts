import { request } from "./client";
import type { EquipmentKey, EquipmentStats, SensorHistory, SensorMetric, SensorRange } from "./types";

export function getSensorHistory(nodeId: string, metric: SensorMetric, range: SensorRange) {
  const search = new URLSearchParams({ metric, range });
  return request<SensorHistory>(`/v1/sensors/${encodeURIComponent(nodeId)}/history?${search}`);
}

export function getEquipmentStats(nodeId: string, equipmentKey: EquipmentKey) {
  return request<EquipmentStats>("/v1/equipment/stats", {
    method: "POST",
    body: {
      node_id: nodeId,
      equipment_key: equipmentKey,
    },
  });
}
