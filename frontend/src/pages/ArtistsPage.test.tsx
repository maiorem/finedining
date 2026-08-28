import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import ArtistsPage from "./ArtistsPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemoryRouter>
          <ArtistsPage />
        </MemoryRouter>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("ArtistsPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("아티스트 목록과 모집 공고를 함께 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
        );
      }
      if (input.startsWith("/api/artists")) {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: [
              {
                id: 1,
                slug: "kim-artist",
                name: "김아무개",
                role: "연출",
                photo: { id: 5, status: "READY", url640: "http://example.com/640.jpg", lqipBase64: null },
              },
            ],
            error: null,
          }),
        );
      }
      return Promise.resolve(
        jsonResponse({
          success: true,
          data: [{ id: 1, title: "배우 모집", body: "이메일로 지원해 주세요." }],
          error: null,
        }),
      );
    });

    renderPage();

    const link = await screen.findByRole("link", { name: /김아무개/ });
    expect(link).toHaveAttribute("href", "/artists/kim-artist");
    expect(link.querySelector("img")).toHaveAttribute("src", "http://example.com/640.jpg");
    expect(await screen.findByText("배우 모집")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "새 아티스트 추가" })).not.toBeInTheDocument();
  });

  it("아티스트가 없으면 빈 상태 문구를 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();

    expect(await screen.findByText("등록된 아티스트가 없습니다.")).toBeInTheDocument();
  });

  it("관리자로 로그인했으면 새 아티스트 추가 버튼이 보인다", async () => {
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

    expect(await screen.findByRole("button", { name: "새 아티스트 추가" })).toBeInTheDocument();
  });
});
