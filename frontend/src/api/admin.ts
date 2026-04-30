import { request } from "./client";
import type { AdminUser, LogPage } from "./types";

export function createNode(nodeId: string, nodeName: string) {
  return request<null>("/v1/admin/nodes", {
    method: "POST",
    body: { nodeId, nodeName },
  });
}

export function updateNode(nodeId: string, nodeName: string) {
  return request<null>(`/v1/admin/nodes/${encodeURIComponent(nodeId)}`, {
    method: "PUT",
    body: { nodeName },
  });
}

export function deleteNode(nodeId: string) {
  return request<null>(`/v1/admin/nodes/${encodeURIComponent(nodeId)}`, { method: "DELETE" });
}

export function listUsers() {
  return request<AdminUser[]>("/v1/admin/users");
}

export function updateUserRole(userId: number, role: string) {
  return request<null>(`/v1/admin/users/${userId}/role`, {
    method: "PUT",
    body: { role },
  });
}

export function getUserNodes(userId: number) {
  return request<string[]>(`/v1/admin/users/${userId}/nodes`);
}

export function setUserNodes(userId: number, nodeIds: string[]) {
  return request<null>(`/v1/admin/users/${userId}/nodes`, {
    method: "PUT",
    body: { nodeIds },
  });
}

export function listLogs(params: {
  page: number;
  size: number;
  type?: string;
  targetId?: string;
  startDate?: string;
  endDate?: string;
}) {
  const search = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
  });
  for (const key of ["type", "targetId", "startDate", "endDate"] as const) {
    if (params[key]) {
      search.set(key, params[key]);
    }
  }
  return request<LogPage>(`/v1/admin/logs?${search}`);
}
