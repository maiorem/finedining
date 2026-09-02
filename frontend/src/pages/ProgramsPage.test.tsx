import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import ProgramsPage from "./ProgramsPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemoryRouter>
          <ProgramsPage />
        </MemoryRouter>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("ProgramsPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("프로그램 목록과 참가하기·위치보기 링크를 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
        );
      }
      return Promise.resolve(
        jsonResponse({
          success: true,
          data: [
            {
              id: 1,
              slug: "summer-tasting",
              title: "여름 시식회",
              description: "참가는 구글폼으로 접수합니다.",
              applyUrl: "https://forms.gle/abcd",
              locationUrl: "https://map.naver.com/p/somewhere",
              thumbnail: null,
            },
          ],
          error: null,
        }),
      );
    });

    renderPage();

    const titleLink = await screen.findByRole("link", { name: /여름 시식회/ });
    expect(titleLink).toHaveAttribute("href", "/programs/summer-tasting");
    expect(screen.getByText("참가는 구글폼으로 접수합니다.")).toBeInTheDocument();

    const applyLink = screen.getByRole("link", { name: /참가하기/ });
    expect(applyLink).toHaveAttribute("href", "https://forms.gle/abcd");
    expect(applyLink).toHaveAttribute("target", "_blank");

    const locationLink = screen.getByRole("link", { name: /위치보기/ });
    expect(locationLink).toHaveAttribute("href", "https://map.naver.com/p/somewhere");

    expect(screen.queryByRole("button", { name: "새 프로그램 추가" })).not.toBeInTheDocument();
  });

  it("프로그램이 없으면 빈 상태 문구를 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();

    expect(await screen.findByText("등록된 프로그램이 없습니다.")).toBeInTheDocument();
  });

  it("관리자로 로그인했으면 새 프로그램 추가 버튼이 보인다", async () => {
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

    expect(await screen.findByRole("button", { name: "새 프로그램 추가" })).toBeInTheDocument();
  });
});
