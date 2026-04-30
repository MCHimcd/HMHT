import { request } from "./client";
import type { HealthData } from "./types";

export function getHealth() {
  return request<HealthData>("/v1/health");
}
