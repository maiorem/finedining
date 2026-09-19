import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import { MemberAuthProvider } from "../../contexts/MemberAuthContext";
import { SiteGate } from "./SiteGate";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

const UNAUTHENTICATED = jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } });
const ADMIN_SESSION = jsonResponse({
  success: true,
  data: { accessToken: "token", username: "admin", role: "SUPER_ADMIN" },
  error: null,
});

function mockFetch(options: { status: "open" | "closed" | "error"; admin: boolean }) {
  const fetchMock = vi.fn((input: string) => {
    if (input === "/api/site/status") {
      if (options.status === "error") return Promise.reject(new Error("network"));
      return Promise.resolve(jsonResponse({ success: true, data: { open: options.status === "open" }, error: null }));
    }
    if (input === "/api/auth/admin/refresh") return Promise.resolve(options.admin ? ADMIN_SESSION : UNAUTHENTICATED);
    return Promise.resolve(UNAUTHENTICATED);
  });
  vi.stubGlobal("fetch", fetchMock);
}

function renderGate(path = "/proposal") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemberAuthProvider>
          <MemoryRouter initialEntries={[path]}>
            <SiteGate>
              <p>사이트 본문</p>
            </SiteGate>
          </MemoryRouter>
        </MemberAuthProvider>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("SiteGate", () => {
  beforeEach(() => vi.restoreAllMocks());
  afterEach(() => vi.unstubAllGlobals());

  it("공개 상태면 본문을 그대로 보여준다", async () => {
    mockFetch({ status: "open", admin: false });
    renderGate();

    expect(await screen.findByText("사이트 본문")).toBeInTheDocument();
  });

  it("비공개이고 관리자가 아니면 준비 중 화면만 보여준다", async () => {
    mockFetch({ status: "closed", admin: false });
    renderGate();

    expect(await screen.findByText("홈페이지 오픈을 준비하고 있습니다.")).toBeInTheDocument();
    expect(screen.queryByText("사이트 본문")).not.toBeInTheDocument();
  });

  it("비공개여도 /login은 열어둔다", async () => {
    mockFetch({ status: "closed", admin: false });
    renderGate("/login");

    expect(await screen.findByRole("button", { name: "ADMIN" })).toBeInTheDocument();
    expect(screen.queryByText("홈페이지 오픈을 준비하고 있습니다.")).not.toBeInTheDocument();
  });

  it("비공개여도 관리자로 로그인되어 있으면 본문을 보여준다", async () => {
    mockFetch({ status: "closed", admin: true });
    renderGate();

    expect(await screen.findByText("사이트 본문")).toBeInTheDocument();
  });

  it("공개 여부를 못 읽으면 화면은 열어둔다(서버가 어차피 막는다)", async () => {
    mockFetch({ status: "error", admin: false });
    renderGate();

    expect(await screen.findByText("사이트 본문")).toBeInTheDocument();
  });
});
