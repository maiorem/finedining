import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import MembersPage from "./MembersPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

const UNAUTHENTICATED = jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } });

function renderPage(fetchMock: ReturnType<typeof vi.fn>) {
  vi.stubGlobal("fetch", fetchMock);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MembersPage />
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("MembersPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("관리자가 아니면 회원 목록을 렌더하지 않는다", async () => {
    const fetchMock = vi.fn((input: string) => {
      expect(input).not.toBe("/api/accounts/manage");
      return Promise.resolve(UNAUTHENTICATED);
    });
    renderPage(fetchMock);

    expect(await screen.findByText("회원 관리")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("관리자로 로그인했으면 회원 목록을 공개 정보와 함께 보여준다", async () => {
    const fetchMock = vi.fn((input: string) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { accessToken: "token", username: "admin", role: "SUPER_ADMIN" },
            error: null,
          }),
        );
      }
      if (input === "/api/accounts/manage") {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: [
              {
                id: 1,
                nickname: "김아무개",
                email: "user@example.com",
                provider: "kakao",
                locale: "KO",
                status: "ACTIVE",
                createdAt: "2026-09-01T00:00:00Z",
              },
            ],
            error: null,
          }),
        );
      }
      return Promise.resolve(UNAUTHENTICATED);
    });

    renderPage(fetchMock);

    expect(await screen.findByText("김아무개")).toBeInTheDocument();
    expect(screen.getByText("user@example.com")).toBeInTheDocument();
    expect(screen.getByText("kakao")).toBeInTheDocument();
    expect(screen.getByText("활동중")).toBeInTheDocument();
  });
});
