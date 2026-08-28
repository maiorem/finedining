import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import BookingPage from "./BookingPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemoryRouter>
          <BookingPage />
        </MemoryRouter>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("BookingPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("비로그인 방문자에게는 회차 관리 토글이 보이지 않는다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();

    expect(await screen.findByText("이 달에는 등록된 회차가 없습니다. 다음 달을 확인해 주세요.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "회차 관리" })).not.toBeInTheDocument();
  });

  it("관리자로 로그인했으면 회차 관리 토글이 보인다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { accessToken: "t", username: "admin", role: "EDITOR" },
            error: null,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();

    expect(await screen.findByRole("button", { name: "회차 관리" })).toBeInTheDocument();
  });
});
