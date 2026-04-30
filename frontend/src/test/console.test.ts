import { describe, expect, it } from "vitest";

import type { EquipmentMeta, SensorHistory } from "../api/types";
import { alertCount, buildDeviceRuntimeSummary, formatMetricValue, latestMetricValue } from "../utils/console";

const equipment: EquipmentMeta[] = [
  { key: "water_pump", name: "水泵", icon: "pump", controllable: true, controlType: "toggle" },
  { key: "led_light", name: "补光灯", icon: "light", controllable: true, controlType: "slider" },
  { key: "fan", name: "排风扇", icon: "fan", controllable: true, controlType: "toggle" },
];

describe("console utils", () => {
  it("builds a consistent device runtime summary", () => {
    const summary = buildDeviceRuntimeSummary(equipment, true);

    expect(summary.total).toBe(3);
    expect(summary.running).toBe(2);
    expect(summary.idle).toBe(1);
    expect(summary.items.map((item) => item.running)).toEqual([1, 0, 1]);
  });

  it("formats latest metric values by metric scale", () => {
    const histories: Partial<Record<"temperature" | "light", SensorHistory>> = {
      temperature: { metric: "temperature", summary: { max: 0, min: 0, avg: 0 }, points: [{ timeLabel: "10:00", value: 25.2 }] },
      light: { metric: "light", summary: { max: 0, min: 0, avg: 0 }, points: [{ timeLabel: "10:00", value: 1024.4 }] },
    };

    expect(formatMetricValue(latestMetricValue(histories, "temperature"), "temperature")).toBe("25.2");
    expect(formatMetricValue(latestMetricValue(histories, "light"), "light")).toBe("1,024");
  });

  it("counts alerts safely when data is missing", () => {
    expect(alertCount(null)).toBe(0);
    expect(alertCount({ total: 1, unhandled_count: 1, handled_recent_count: 0, items: [{} as never] })).toBe(1);
  });
});
