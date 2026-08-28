import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import ArtistDetailPage from "./ArtistDetailPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/artists/:slug" element={<ArtistDetailPage />} />
          </Routes>
        </MemoryRouter>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("ArtistDetailPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("비로그인 방문자에게 이름·사진·소개·참여 작품(자유 텍스트)을 보여주고 편집 토글은 없다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
        );
      }
      return Promise.resolve(
        jsonResponse({
          success: true,
          data: {
            id: 1,
            slug: "kim-artist",
            name: "김아무개",
            role: "연출",
            bio: "소개 문구",
            credits: "쇼케이스 출연 (2024)\n다른 극단 공연 참여 (2023)",
            linkUrl: "https://instagram.com/kimartist",
            photo: {
              id: 5,
              status: "READY",
              altText: "프로필 사진",
              url640: "http://example.com/640.jpg",
              width: 640,
              height: 640,
            },
          },
          error: null,
        }),
      );
    });

    renderAt("/artists/kim-artist");

    expect(await screen.findByRole("heading", { name: "김아무개" })).toBeInTheDocument();
    expect(screen.getByText("소개 문구")).toBeInTheDocument();
    // RTL의 기본 텍스트 매칭은 공백을 정규화한다(줄바꿈 → 스페이스) — pre-wrap으로 렌더된 실제
    // 줄바꿈은 스냅샷이 아니라 정규화 여부 자체로 충분히 검증된다.
    expect(screen.getByText("쇼케이스 출연 (2024) 다른 극단 공연 참여 (2023)")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "프로필 사진" })).toHaveAttribute("src", "http://example.com/640.jpg");
    expect(screen.getByRole("link", { name: /새 창에서 열림/ })).toHaveAttribute("target", "_blank");
    expect(screen.queryByRole("button", { name: "편집 모드 켜기" })).not.toBeInTheDocument();
  });

  it("존재하지 않는 아티스트면 안내 문구를 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
        );
      }
      return Promise.resolve(
        jsonResponse({ success: false, data: null, error: { code: "ENTITY_NOT_FOUND", message: "x" } }),
      );
    });

    renderAt("/artists/unknown");

    expect(await screen.findByText("존재하지 않는 아티스트입니다.")).toBeInTheDocument();
  });

  it("관리자로 로그인했으면 편집 모드 토글이 보인다", async () => {
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
      return Promise.resolve(
        jsonResponse({
          success: true,
          data: {
            id: 1,
            slug: "kim-artist",
            name: "김아무개",
            role: null,
            bio: null,
            credits: null,
            linkUrl: null,
            photo: null,
          },
          error: null,
        }),
      );
    });

    renderAt("/artists/kim-artist");

    expect(await screen.findByRole("button", { name: "편집 모드 켜기" })).toBeInTheDocument();
  });
});
