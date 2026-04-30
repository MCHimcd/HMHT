import type { ApiResponse } from "./types";

export class ApiError extends Error {
  readonly code: number;

  constructor(code: number, message: string) {
    super(message || "请求失败");
    this.name = "ApiError";
    this.code = code;
  }
}

const defaultBaseUrl = "/api";
export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || defaultBaseUrl;

let authToken = "";
let unauthorizedHandler: (() => void) | null = null;

export function setAuthToken(token: string) {
  authToken = token;
}

export function setUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler;
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (authToken) {
    headers.set("Authorization", `Bearer ${authToken}`);
  }

  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError(0, "服务不可用，请确认后端已启动");
  }

  let payload: ApiResponse<T>;
  try {
    payload = (await response.json()) as ApiResponse<T>;
  } catch {
    throw new ApiError(response.status, "响应格式异常");
  }

  if (payload.code !== 200) {
    if (payload.code === 401) {
      unauthorizedHandler?.();
    }
    throw new ApiError(payload.code, payload.message);
  }

  return payload.data;
}
