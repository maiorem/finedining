import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import SiteVisibilityControl from "./SiteVisibilityControl";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

const ADMIN_SESSION = jsonResponse({
  success: true,
  data: { accessToken: "token", username: "admin", role: "SUPER_ADMIN" },
  error: null,
});

function setup(options: { open: boolean; putResponse?: Response }) {
  const fetchMock = vi.fn((input: string, init?: RequestInit) => {
    if (input === "/api/auth/admin/refresh") return Promise.resolve(ADMIN_SESSION);
    if (input === "/api/site/status") {
      return Promise.resolve(jsonResponse({ success: true, data: { open: options.open }, error: null }));
    }
    if (input === "/api/site/visibility" && init?.method === "PUT") {
      return Promise.resolve(
        options.putResponse ?? jsonResponse({ success: true, data: { open: !options.open }, error: null }),
      );
    }
    return Promise.resolve(jsonResponse({ success: true, data: null, error: null }));
  });
  vi.stubGlobal("fetch", fetchMock);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <SiteVisibilityControl />
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
  return fetchMock;
}

describe("SiteVisibilityControl", () => {
  beforeEach(() => vi.restoreAllMocks());
  afterEach(() => vi.unstubAllGlobals());

  it("비공개 상태를 보여주고 공개하기 버튼을 누르면 확인 후 요청을 보낸다", async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const fetchMock = setup({ open: false });

    expect(await screen.findByText(/현재 비공개 상태입니다/)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "사이트 공개하기" }));

    expect(confirmSpy).toHaveBeenCalled();
    await waitFor(() => {
      const call = fetchMock.mock.calls.find(([input, init]) => input === "/api/site/visibility" && init?.method === "PUT");
      expect(JSON.parse(call![1]!.body as string)).toEqual({ open: true });
    });
    expect(await screen.findByText(/현재 공개 상태입니다/)).toBeInTheDocument();
  });

  it("공개 확인을 취소하면 요청을 보내지 않는다", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const fetchMock = setup({ open: false });

    await user.click(await screen.findByRole("button", { name: "사이트 공개하기" }));

    expect(fetchMock.mock.calls.some(([input, init]) => input === "/api/site/visibility" && init?.method === "PUT")).toBe(
      false,
    );
  });

  it("PIN 확인이 필요하면 PIN 입력창을 띄운다", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    setup({
      open: false,
      putResponse: jsonResponse({ success: false, data: null, error: { code: "PIN_REQUIRED", message: "x" } }),
    });

    await user.click(await screen.findByRole("button", { name: "사이트 공개하기" }));

    expect(await screen.findByLabelText("6자리 PIN")).toBeInTheDocument();
  });

  it("공개 상태에서는 비공개로 전환 버튼이 보이고 확인창 없이 요청한다", async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, "confirm");
    setup({ open: true });

    await user.click(await screen.findByRole("button", { name: "비공개로 전환" }));

    expect(confirmSpy).not.toHaveBeenCalled();
    expect(await screen.findByText(/현재 비공개 상태입니다/)).toBeInTheDocument();
  });
});
