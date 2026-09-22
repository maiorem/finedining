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
            slug: "sample",
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

    renderAt("/productions/sample");

    expect(await screen.findByRole("heading", { name: "쇼케이스" })).toBeInTheDocument();
    expect(screen.getByText("부제")).toBeInTheDocument();
    expect(screen.getByText("9코스로 이어지는 공연형 다이닝입니다.")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "무대 사진" })).toHaveAttribute("src", "http://example.com/640.jpg");
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
            slug: "sample",
            title: "쇼케이스",
            subtitle: "부제",
            description: "설명",
            images: [
              { id: 1, status: "READY", altText: "히어로 사진", caption: null, url1600: "http://example.com/1-1600.jpg" },
              {
                id: 2,
                status: "READY",
                altText: "두번째 사진 대체텍스트",
                caption: "두번째 사진 설명 문단입니다.",
                url1600: "http://example.com/2-1600.jpg",
              },
              { id: 3, status: "READY", altText: "세번째 사진 대체텍스트", caption: null, url1600: "http://example.com/3-1600.jpg" },
            ],
          },
          error: null,
        }),
      );
    });

    renderAt("/productions/sample");

    expect(await screen.findByRole("img", { name: "히어로 사진" })).toHaveAttribute(
      "src",
      "http://example.com/1-1600.jpg",
    );
    expect(screen.getByRole("img", { name: "두번째 사진 대체텍스트" })).toHaveAttribute(
      "src",
      "http://example.com/2-1600.jpg",
    );
    expect(screen.getByRole("img", { name: "세번째 사진 대체텍스트" })).toHaveAttribute(
      "src",
      "http://example.com/3-1600.jpg",
    );
    expect(screen.getByText("두번째 사진 설명 문단입니다.")).toBeInTheDocument(); // figcaption
    expect(screen.queryByText("세번째 사진 대체텍스트")).not.toBeInTheDocument(); // 설명이 없으면 캡션을 렌더하지 않는다
  });

  it("예약·위치 링크가 있으면 새 창으로 여는 외부 링크 버튼을 보여준다", async () => {
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
            slug: "sample",
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

    renderAt("/productions/sample");

    const bookingLink = await screen.findByRole("link", { name: /예약하기/ });
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
          data: { id: 1, slug: "sample", title: "쇼케이스", subtitle: null, images: [] },
          error: null,
        }),
      );
    });

    renderAt("/productions/sample");

    expect(await screen.findByRole("button", { name: "편집 모드 켜기" })).toBeInTheDocument();
  });

  it("아버지의 식탁 슬러그면 전용 상세를 보여주고 예약 링크는 DB 값을 쓴다", async () => {
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
            title: "아버지의 식탁",
            subtitle: null,
            description: null,
            bookingUrl: "https://booking.naver.com/x",
            locationUrl: "https://map.naver.com/y",
            images: [],
          },
          error: null,
        }),
      );
    });

    renderAt("/productions/showcase");

    expect(await screen.findByRole("heading", { level: 1 })).toHaveTextContent("아버지의 삶을 맛.보.는.");
    const reserve = screen.getAllByRole("link", { name: /예약하기/ })[0];
    expect(reserve).toHaveAttribute("href", "https://booking.naver.com/x");
    expect(reserve).toHaveAttribute("target", "_blank");
    expect(screen.getByRole("link", { name: /위치보기/ })).toHaveAttribute("href", "https://map.naver.com/y");
  });

  it("아버지의 식탁은 네이버·놀 예약 링크가 둘 다 있으면 두 버튼을 각각 보여준다", async () => {
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
            title: "아버지의 식탁",
            subtitle: null,
            description: null,
            bookingUrl: "https://booking.naver.com/booking/12/bizes/1741081",
            nolBookingUrl: "https://nol.yanolja.com/ticket/products/26013867",
            locationUrl: null,
            images: [],
          },
          error: null,
        }),
      );
    });

    renderAt("/productions/showcase");

    await screen.findByRole("heading", { level: 1 });
    const naverLinks = screen.getAllByRole("link", { name: /네이버 예약하기/ });
    const nolLinks = screen.getAllByRole("link", { name: /놀\(NOL\) 예약하기/ });
    expect(naverLinks.length).toBeGreaterThan(0);
    expect(nolLinks.length).toBeGreaterThan(0);
    for (const link of naverLinks) {
      expect(link).toHaveAttribute("href", "https://booking.naver.com/booking/12/bizes/1741081");
      expect(link).toHaveAttribute("target", "_blank");
    }
    for (const link of nolLinks) {
      expect(link).toHaveAttribute("href", "https://nol.yanolja.com/ticket/products/26013867");
    }
  });

  it("고정 상세가 아닌 작품은 설명을 마크다운으로 그리고 본문에 넣은 사진은 갤러리에서 뺀다", async () => {
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
            id: 2,
            slug: "sample",
            title: "새 공연",
            subtitle: null,
            description: "**어떤 공연인가요?**\n\n![본문 사진](image:20)\n\n끝",
            bookingUrl: null,
            locationUrl: null,
            images: [
              { id: 10, status: "READY", altText: "히어로", url640: "http://example.com/hero.jpg", width: 640, height: 400 },
              { id: 20, status: "READY", altText: "본문용", url640: "http://example.com/inline.jpg", width: 640, height: 400 },
            ],
          },
          error: null,
        }),
      );
    });

    renderAt("/productions/sample");

    expect(await screen.findByRole("heading", { level: 2, name: "어떤 공연인가요?" })).toBeInTheDocument();
    expect(screen.getAllByRole("img", { name: "본문 사진" })).toHaveLength(1);
    expect(screen.getByRole("img", { name: "히어로" })).toBeInTheDocument();
  });

  it("고정 상세의 사진 구성: 노동 이야기는 사진 한 장, 돈까스와 공연 순간은 갤러리다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
        );
      }
      return Promise.resolve(
        jsonResponse({
          success: true,
          data: { id: 1, slug: "showcase", title: "아버지의 식탁", subtitle: null, description: null, bookingUrl: null, locationUrl: null, images: [] },
          error: null,
        }),
      );
    });

    renderAt("/productions/showcase");

    await screen.findByRole("heading", { level: 1 });
    // 노동 이야기: 요리 장면 사진 한 장(갤러리 아님)
    expect(screen.getByRole("img", { name: "주방에서 요리하는 장면" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "주방에서 요리하는 장면" })).not.toBeInTheDocument();
    // 이 돈까스에는…: 노동의 기억 1·2 갤러리
    expect(screen.getByRole("button", { name: "한 사람의 노동의 기억 1" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "한 사람의 노동의 기억 2" })).toBeInTheDocument();
    // 삶이 공연이 되는 순간: 공연 장면 1·2·3 갤러리
    for (const n of [1, 2, 3]) {
      expect(screen.getByRole("button", { name: `공연 장면 ${n}` })).toBeInTheDocument();
    }
    // 아버지 이야기: 사진 3장 갤러리
    expect(screen.getAllByRole("button", { name: /아버지의 사진/ })).toHaveLength(3);
  });
});
