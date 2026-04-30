import { describe, expect, it } from "vitest";

import type { TaskRequest } from "../api/types";
import { validateTaskRequest } from "../utils/tasks";

const baseTask: TaskRequest = {
  nodeId: "SIM_Node_01",
  equipmentKey: "water_pump",
  action: "turn_on",
  taskName: "每日灌溉",
  hour: 8,
  minute: 30,
  repeatDays: "1,2,3,4,5",
};

describe("validateTaskRequest", () => {
  it("accepts a valid task", () => {
    expect(validateTaskRequest(baseTask)).toBe("");
  });

  it("rejects invalid hour", () => {
    expect(validateTaskRequest({ ...baseTask, hour: 24 })).toContain("小时");
  });

  it("rejects missing repeat days", () => {
    expect(validateTaskRequest({ ...baseTask, repeatDays: "" })).toContain("重复日期");
  });
});
