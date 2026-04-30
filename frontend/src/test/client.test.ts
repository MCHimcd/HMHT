import { afterEach, describe, expect, it, vi } from "vitest";

import { ApiError, request, setAuthToken, setUnauthorizedHandler } from "../api/client";

describe("api client", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    setAuthToken("");
    setUnauthorizedHandler(() => undefined);
  });

  it("unwraps successful ApiResponse data", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ code: 200, message: "success", data: { ok: true } }),
      }),
    );

    await expect(request<{ ok: boolean }>("/v1/health")).resolves.toEqual({ ok: true });
  });

  it("throws ApiError and calls unauthorized handler on code 401", async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ code: 401, message: "token invalid", data: null }),
      }),
    );

    await expect(request("/v1/auth/profile")).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it("attaches bearer token when present", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      json: () => Promise.resolve({ code: 200, message: "success", data: null }),
    });
    vi.stubGlobal("fetch", fetchMock);
    setAuthToken("abc-token");

    await request("/v1/tasks");

    const [, options] = fetchMock.mock.calls[0];
    expect((options.headers as Headers).get("Authorization")).toBe("Bearer abc-token");
  });
});
