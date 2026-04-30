import type { EquipmentKey, SensorMetric, SensorRange } from "../api/types";

export const defaultNodeId = "SIM_Node_01";

export const sensorOptions: Array<{ key: SensorMetric; label: string; unit: string }> = [
  { key: "temperature", label: "空气温度", unit: "C" },
  { key: "humidity", label: "空气湿度", unit: "%" },
  { key: "light", label: "光照强度", unit: "Lux" },
  { key: "soilHumidity", label: "土壤湿度", unit: "%" },
];

export const rangeOptions: Array<{ key: SensorRange; label: string }> = [
  { key: "24h", label: "24小时" },
  { key: "7d", label: "7天" },
  { key: "30d", label: "30天" },
];

export const equipmentOptions: Array<{ key: EquipmentKey; label: string; actionOn: string; actionOff: string }> = [
  { key: "water_pump", label: "水泵", actionOn: "开启灌溉", actionOff: "关闭灌溉" },
  { key: "led_light", label: "补光灯", actionOn: "开启补光", actionOff: "关闭补光" },
  { key: "fan", label: "排风扇", actionOn: "开启通风", actionOff: "关闭通风" },
];

export function sensorLabel(key: string) {
  return sensorOptions.find((item) => item.key === key)?.label || key;
}

export function sensorUnit(key: string) {
  return sensorOptions.find((item) => item.key === key)?.unit || "";
}

export function equipmentLabel(key: string) {
  return equipmentOptions.find((item) => item.key === key)?.label || key;
}

export function actionLabel(action: string) {
  if (action === "turn_on") return "开启";
  if (action === "turn_off") return "关闭";
  return action;
}
