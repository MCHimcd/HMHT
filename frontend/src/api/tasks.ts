import { request } from "./client";
import type { TaskInfo, TaskRequest } from "./types";

export function listTasks() {
  return request<TaskInfo[]>("/v1/tasks");
}

export function createTask(payload: TaskRequest) {
  return request<TaskInfo>("/v1/tasks", {
    method: "POST",
    body: payload,
  });
}

export function updateTask(taskId: number, payload: TaskRequest) {
  return request<TaskInfo>(`/v1/tasks/${taskId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deleteTask(taskId: number) {
  return request<null>(`/v1/tasks/${taskId}`, { method: "DELETE" });
}

export function toggleTask(taskId: number) {
  return request<null>(`/v1/tasks/${taskId}/toggle`, { method: "PUT" });
}
