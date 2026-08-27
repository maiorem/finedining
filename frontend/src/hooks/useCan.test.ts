import { renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import { useCan } from "./useCan";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

describe("useCan", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("로그인하지 않았으면 false를 반환한다", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
    );

    const { result } = renderHook(() => useCan("production:edit"), { wrapper: AdminAuthProvider });

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(result.current).toBe(false);
  });

  it("EDITOR로 로그인했으면 true를 반환한다", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ success: true, data: { accessToken: "t", username: "admin", role: "EDITOR" }, error: null }),
    );

    const { result } = renderHook(() => useCan("production:edit"), { wrapper: AdminAuthProvider });

    await waitFor(() => expect(result.current).toBe(true));
  });
});
