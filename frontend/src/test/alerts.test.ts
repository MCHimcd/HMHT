import { describe, expect, it } from "vitest";

import { normalizeAlertList } from "../api/alerts";

describe("alert api helpers", () => {
  it("normalizes camelCase alert fields from backend responses", () => {
    const result = normalizeAlertList({
      total: 1,
      unhandledCount: 1,
      handledRecentCount: 0,
      items: [
        {
          alertId: "ALT-001",
          nodeId: "SIM_Node_01",
          typeName: "空气温度",
          sourceName: "温度传感器",
          isHandled: false,
          handleTime: null,
          timestamp: 1,
          type: "temperature",
          level: "warning",
          message: "温度过低",
          source: "sensor",
        },
      ],
    });

    expect(result.unhandled_count).toBe(1);
    expect(result.items[0].alert_id).toBe("ALT-001");
    expect(result.items[0].is_handled).toBe(false);
  });
});
