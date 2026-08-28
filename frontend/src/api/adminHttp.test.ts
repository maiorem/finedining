import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "./http";
import { refreshAdminSession } from "./auth";
import { apiAdminGet, registerAdminSessionHandlers } from "./adminHttp";

vi.mock("./auth", () => ({
  refreshAdminSession: vi.fn(),
}));

function jsonResponse(status: number, body: unknown): Response {
  return { status, json: async () => body } as Response;
}

describe("adminHttp — 401 만료 시 조용히 재발급 후 재시도한다", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.mocked(refreshAdminSession).mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("첫 요청이 401이면 refresh 후 새 토큰으로 재시도해 성공한다", async () => {
    vi.mocked(refreshAdminSession).mockResolvedValue({
      accessToken: "fresh-token",
      username: "admin",
      role: "EDITOR",
    });

    fetchMock
      .mockResolvedValueOnce(jsonResponse(401, { success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }))
      .mockResolvedValueOnce(jsonResponse(200, { success: true, data: { ok: true }, error: null }));

    const onRefreshed = vi.fn();
    registerAdminSessionHandlers({ onRefreshed, onExpired: vi.fn() });

    const result = await apiAdminGet<{ ok: boolean }>("/api/artists/manage/1", "expired-token");

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe("Bearer fresh-token");
    expect(onRefreshed).toHaveBeenCalledWith(expect.objectContaining({ accessToken: "fresh-token" }));
  });

  it("refresh마저 실패하면 세션을 비우고 에러를 그대로 던진다", async () => {
    vi.mocked(refreshAdminSession).mockRejectedValue(new ApiError("UNAUTHORIZED", "만료됨"));

    fetchMock.mockResolvedValueOnce(
      jsonResponse(401, { success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
    );

    const onExpired = vi.fn();
    registerAdminSessionHandlers({ onRefreshed: vi.fn(), onExpired });

    await expect(apiAdminGet("/api/artists/manage/1", "expired-token")).rejects.toBeInstanceOf(ApiError);
    expect(onExpired).toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("401이 아니면 재시도 없이 그대로 처리한다", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { success: true, data: { ok: true }, error: null }));

    const result = await apiAdminGet<{ ok: boolean }>("/api/artists/manage/1", "valid-token");

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
