import type { AlertList, EquipmentKey, EquipmentMeta, SensorHistory, SensorMetric } from "../api/types";

import { equipmentLabel } from "./domain";

export type SensorHistoryMap = Partial<Record<SensorMetric, SensorHistory>>;
export type DeviceRuntimeState = "running" | "idle" | "offline" | "fault";

export type DeviceRuntimeItem = {
  key: EquipmentKey;
  name: string;
  total: number;
  running: number;
  state: DeviceRuntimeState;
};

export type DeviceRuntimeSummary = {
  total: number;
  running: number;
  idle: number;
  offline: number;
  fault: number;
  items: DeviceRuntimeItem[];
};

const fallbackEquipment: EquipmentMeta[] = [
  { key: "fan", name: "排风扇", icon: "fan", controllable: true, controlType: "toggle" },
  { key: "led_light", name: "补光灯", icon: "light", controllable: true, controlType: "slider" },
  { key: "water_pump", name: "水泵", icon: "pump", controllable: true, controlType: "toggle" },
];

const runtimeBlueprint: Record<EquipmentKey, DeviceRuntimeState> = {
  fan: "running",
  led_light: "idle",
  water_pump: "running",
};

export function latestMetricValue(histories: SensorHistoryMap, metric: SensorMetric) {
  return histories[metric]?.points.at(-1)?.value;
}

export function formatMetricValue(value: number | undefined, metric: SensorMetric) {
  if (value == null) return "-";
  if (metric === "light") return Math.round(value).toLocaleString("zh-CN");
  return value.toFixed(1).replace(/\.0$/, "");
}

export function buildDeviceRuntimeSummary(equipment: EquipmentMeta[], hasActiveAlert = false): DeviceRuntimeSummary {
  const source = equipment.length ? equipment : fallbackEquipment;
  const items = source.map((item) => {
    let state = runtimeBlueprint[item.key];
    if (item.key === "fan" && !hasActiveAlert) {
      state = "idle";
    }
    return {
      key: item.key,
      name: item.name || equipmentLabel(item.key),
      total: 1,
      running: state === "running" ? 1 : 0,
      state,
    };
  });

  return {
    total: items.length,
    running: items.filter((item) => item.state === "running").length,
    idle: items.filter((item) => item.state === "idle").length,
    offline: items.filter((item) => item.state === "offline").length,
    fault: items.filter((item) => item.state === "fault").length,
    items,
  };
}

export function alertCount(alerts: AlertList | null) {
  return alerts?.items.length ?? 0;
}
