import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import ProductionDetailPage from "./ProductionDetailPage";

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
            <Route path="/productions/:slug" element={<ProductionDetailPage />} />
          </Routes>
        </MemoryRouter>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("ProductionDetailPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("비로그인 방문자에게 제목·부제·이미지를 보여주고 편집 토글은 없다", async () => {
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
            slug: "showcase",
            title: "쇼케이스",
            subtitle: "부제",
            description: "9코스로 이어지는 공연형 다이닝입니다.",
            images: [
              {
                id: 2,
                status: "READY",
                altText: "무대 사진",
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

    renderAt("/productions/showcase");

    expect(await screen.findByRole("heading", { name: "쇼케이스" })).toBeInTheDocument();
    expect(screen.getByText("부제")).toBeInTheDocument();
    expect(screen.getByText("9코스로 이어지는 공연형 다이닝입니다.")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "무대 사진" })).toHaveAttribute("src", "http://example.com/640.jpg");
    expect(screen.queryByRole("button", { name: "편집 모드 켜기" })).not.toBeInTheDocument();
  });

  it("이미지가 여러 장이면 첫 장은 히어로로, 나머지는 에디토리얼 섹션에 캡션과 함께 보여준다", async () => {
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
            slug: "showcase",
            title: "쇼케이스",
            subtitle: "부제",
            description: "설명",
            images: [
              { id: 1, status: "READY", altText: "히어로 사진", url1600: "http://example.com/1-1600.jpg" },
              { id: 2, status: "READY", altText: "두번째 사진", url1600: "http://example.com/2-1600.jpg" },
              { id: 3, status: "READY", altText: "세번째 사진", url1600: "http://example.com/3-1600.jpg" },
            ],
          },
          error: null,
        }),
      );
    });

    renderAt("/productions/showcase");

    expect(await screen.findByRole("img", { name: "히어로 사진" })).toHaveAttribute(
      "src",
      "http://example.com/1-1600.jpg",
    );
    expect(screen.getByRole("img", { name: "두번째 사진" })).toHaveAttribute("src", "http://example.com/2-1600.jpg");
    expect(screen.getByRole("img", { name: "세번째 사진" })).toHaveAttribute("src", "http://example.com/3-1600.jpg");
    expect(screen.getByText("두번째 사진")).toBeInTheDocument(); // figcaption
    expect(screen.getByText("세번째 사진")).toBeInTheDocument();
  });

  it("예매·위치 링크가 있으면 새 창으로 여는 외부 링크 버튼을 보여준다", async () => {
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
            slug: "showcase",
            title: "쇼케이스",
            subtitle: null,
            description: null,
            bookingUrl: "https://booking.naver.com/bizes/1",
            locationUrl: "https://map.naver.com/p/somewhere",
            images: [],
          },
          error: null,
        }),
      );
    });

    renderAt("/productions/showcase");

    const bookingLink = await screen.findByRole("link", { name: /예매하기/ });
    expect(bookingLink).toHaveAttribute("href", "https://booking.naver.com/bizes/1");
    expect(bookingLink).toHaveAttribute("target", "_blank");
    expect(bookingLink).toHaveAttribute("rel", "noopener noreferrer");

    const locationLink = screen.getByRole("link", { name: /위치보기/ });
    expect(locationLink).toHaveAttribute("href", "https://map.naver.com/p/somewhere");
  });

  it("존재하지 않는 작품이면 안내 문구를 보여준다", async () => {
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

    renderAt("/productions/unknown");

    expect(await screen.findByText("존재하지 않는 작품입니다.")).toBeInTheDocument();
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
          data: { id: 1, slug: "showcase", title: "쇼케이스", subtitle: null, images: [] },
          error: null,
        }),
      );
    });

    renderAt("/productions/showcase");

    expect(await screen.findByRole("button", { name: "편집 모드 켜기" })).toBeInTheDocument();
  });
});
