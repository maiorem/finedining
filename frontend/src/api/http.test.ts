import { afterEach, describe, expect, it, vi } from "vitest";
import { apiGet, registerPublicAuthorizer } from "./http";

function envelope(success: boolean, data: unknown, status = 200): Response {
  const body = success
    ? { success: true, data, error: null }
    : { success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } };
  return { status, json: async () => body } as Response;
}

describe("apiGet", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    registerPublicAuthorizer({ getToken: () => null, refreshToken: async () => null });
  });

  it("토큰이 있으면 Authorization 헤더를 싣는다", async () => {
    const fetchMock = vi.fn().mockResolvedValue(envelope(true, [1]));
    vi.stubGlobal("fetch", fetchMock);
    registerPublicAuthorizer({ getToken: () => "t1", refreshToken: async () => null });

    await expect(apiGet("/api/x")).resolves.toEqual([1]);
    expect(fetchMock).toHaveBeenCalledWith("/api/x", { headers: { Authorization: "Bearer t1" } });
  });

  it("401이면 재발급한 토큰으로 한 번 재시도한다", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(envelope(false, null, 401))
      .mockResolvedValueOnce(envelope(true, "ok"));
    vi.stubGlobal("fetch", fetchMock);
    registerPublicAuthorizer({ getToken: () => "old", refreshToken: async () => "new" });

    await expect(apiGet("/api/x")).resolves.toBe("ok");
    expect(fetchMock).toHaveBeenLastCalledWith("/api/x", { headers: { Authorization: "Bearer new" } });
  });

  it("토큰이 없으면 헤더 없이 요청하고 401을 재시도하지 않는다", async () => {
    const fetchMock = vi.fn().mockResolvedValue(envelope(false, null, 401));
    vi.stubGlobal("fetch", fetchMock);
    registerPublicAuthorizer({ getToken: () => null, refreshToken: async () => "new" });

    await expect(apiGet("/api/x")).rejects.toMatchObject({ code: "UNAUTHORIZED" });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
