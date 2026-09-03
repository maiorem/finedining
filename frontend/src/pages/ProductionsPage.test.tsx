import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import ProductionsPage from "./ProductionsPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemoryRouter>
          <ProductionsPage />
        </MemoryRouter>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

function mockUnauthenticatedThen(data: unknown) {
  return (input: string) => {
    if (input.includes("/api/auth/admin/refresh")) {
      return Promise.resolve(
        jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
      );
    }
    return Promise.resolve(jsonResponse({ success: true, data, error: null }));
  };
}

describe("ProductionsPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("작품 목록을 썸네일과 함께 보여준다", async () => {
    fetchMock.mockImplementation(
      mockUnauthenticatedThen([
        {
          id: 1,
          slug: "showcase",
          title: "쇼케이스",
          bookingUrl: null,
          locationUrl: null,
          thumbnail: { id: 2, status: "READY", url640: "http://example.com/640.jpg", lqipBase64: null },
        },
      ]),
    );

    renderPage();

    const link = await screen.findByRole("link", { name: /쇼케이스/ });
    expect(link).toHaveAttribute("href", "/productions/showcase");
    // 썸네일은 인접한 제목이 이미 정보를 전달하는 장식 이미지라 alt=""다(§8.8) — role이 아니라
    // DOM에서 직접 확인한다.
    expect(link.querySelector("img")).toHaveAttribute("src", "http://example.com/640.jpg");
    expect(screen.queryByRole("button", { name: "새 작품 추가" })).not.toBeInTheDocument();
  });

  it("작품마다 예약하기·위치보기 링크가 있으면 목록에 바로 보여준다", async () => {
    fetchMock.mockImplementation(
      mockUnauthenticatedThen([
        {
          id: 1,
          slug: "showcase",
          title: "쇼케이스",
          bookingUrl: "https://booking.naver.com/bizes/1",
          locationUrl: "https://map.naver.com/p/somewhere",
          thumbnail: null,
        },
      ]),
    );

    renderPage();

    const bookingLink = await screen.findByRole("link", { name: /예약하기/ });
    expect(bookingLink).toHaveAttribute("href", "https://booking.naver.com/bizes/1");
    expect(bookingLink).toHaveAttribute("target", "_blank");
    expect(bookingLink).toHaveAttribute("rel", "noopener noreferrer");

    const locationLink = screen.getByRole("link", { name: /위치보기/ });
    expect(locationLink).toHaveAttribute("href", "https://map.naver.com/p/somewhere");
  });

  it("작품이 없으면 빈 상태 문구를 보여준다", async () => {
    fetchMock.mockImplementation(mockUnauthenticatedThen([]));

    renderPage();

    expect(await screen.findByText("등록된 작품이 없습니다. 다음 시즌 작품을 준비하고 있습니다.")).toBeInTheDocument();
  });

  it("관리자로 로그인했으면 새 작품 추가 버튼이 보인다", async () => {
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

    expect(await screen.findByRole("button", { name: "새 작품 추가" })).toBeInTheDocument();
  });
});
