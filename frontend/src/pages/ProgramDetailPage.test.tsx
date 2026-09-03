import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import ProgramDetailPage from "./ProgramDetailPage";

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
            <Route path="/programs/:slug" element={<ProgramDetailPage />} />
          </Routes>
        </MemoryRouter>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("ProgramDetailPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("비로그인 방문자에게 제목·설명·이미지를 보여주고 편집 토글은 없다", async () => {
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
            slug: "summer-tasting",
            title: "여름 시식회",
            description: "성북천 달리기 이벤트입니다.",
            applyUrl: null,
            locationUrl: null,
            images: [
              {
                id: 2,
                status: "READY",
                altText: "행사 사진",
                url640: "http://example.com/640.jpg",
                width: 640,
                height: 400,
              },
            ],
          },
          error: null,
        }),
      );
    });

    renderAt("/programs/summer-tasting");

    expect(await screen.findByRole("heading", { name: "여름 시식회" })).toBeInTheDocument();
    expect(screen.getByText("성북천 달리기 이벤트입니다.")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "행사 사진" })).toHaveAttribute("src", "http://example.com/640.jpg");
    expect(screen.queryByRole("button", { name: "편집 모드 켜기" })).not.toBeInTheDocument();
  });

  it("이미지가 여러 장이면 첫 장은 히어로로, 나머지는 블로그처럼 이미지와 설명 문단을 위아래로 보여준다", async () => {
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
            slug: "summer-tasting",
            title: "여름 시식회",
            description: "설명",
            applyUrl: null,
            locationUrl: null,
            images: [
              { id: 1, status: "READY", altText: "히어로 사진", caption: null, url1600: "http://example.com/1-1600.jpg" },
              {
                id: 2,
                status: "READY",
                altText: "두번째 사진 대체텍스트",
                caption: "두번째 사진 설명 문단입니다.",
                url1600: "http://example.com/2-1600.jpg",
              },
            ],
          },
          error: null,
        }),
      );
    });

    renderAt("/programs/summer-tasting");

    expect(await screen.findByRole("img", { name: "히어로 사진" })).toHaveAttribute(
      "src",
      "http://example.com/1-1600.jpg",
    );
    expect(screen.getByRole("img", { name: "두번째 사진 대체텍스트" })).toHaveAttribute(
      "src",
      "http://example.com/2-1600.jpg",
    );
    expect(screen.getByText("두번째 사진 설명 문단입니다.")).toBeInTheDocument(); // figcaption
  });

  it("참가·위치 링크가 있으면 새 창으로 여는 외부 링크 버튼을 보여준다", async () => {
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
            slug: "summer-tasting",
            title: "여름 시식회",
            description: null,
            applyUrl: "https://forms.gle/abcd",
            locationUrl: "https://map.naver.com/p/somewhere",
            images: [],
          },
          error: null,
        }),
      );
    });

    renderAt("/programs/summer-tasting");

    const applyLink = await screen.findByRole("link", { name: /참가하기/ });
    expect(applyLink).toHaveAttribute("href", "https://forms.gle/abcd");
    expect(applyLink).toHaveAttribute("target", "_blank");
    expect(applyLink).toHaveAttribute("rel", "noopener noreferrer");

    const locationLink = screen.getByRole("link", { name: /위치보기/ });
    expect(locationLink).toHaveAttribute("href", "https://map.naver.com/p/somewhere");
  });

  it("존재하지 않는 프로그램이면 안내 문구를 보여준다", async () => {
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

    renderAt("/programs/unknown");

    expect(await screen.findByText("존재하지 않는 프로그램입니다.")).toBeInTheDocument();
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
          data: { id: 1, slug: "summer-tasting", title: "여름 시식회", description: null, images: [] },
          error: null,
        }),
      );
    });

    renderAt("/programs/summer-tasting");

    expect(await screen.findByRole("button", { name: "편집 모드 켜기" })).toBeInTheDocument();
  });
});
