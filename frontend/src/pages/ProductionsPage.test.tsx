import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import ProductionsPage from "./ProductionsPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ProductionsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
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
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        success: true,
        data: [
          {
            id: 1,
            slug: "showcase",
            title: "쇼케이스",
            thumbnail: { id: 2, status: "READY", url640: "http://example.com/640.jpg", lqipBase64: null },
          },
        ],
        error: null,
      }),
    );

    renderPage();

    const link = await screen.findByRole("link", { name: /쇼케이스/ });
    expect(link).toHaveAttribute("href", "/productions/showcase");
    // 썸네일은 인접한 제목이 이미 정보를 전달하는 장식 이미지라 alt=""다(§8.8) — role이 아니라
    // DOM에서 직접 확인한다.
    expect(link.querySelector("img")).toHaveAttribute("src", "http://example.com/640.jpg");
  });

  it("작품이 없으면 빈 상태 문구를 보여준다", async () => {
    fetchMock.mockResolvedValue(jsonResponse({ success: true, data: [], error: null }));

    renderPage();

    expect(await screen.findByText("등록된 작품이 없습니다. 다음 시즌 작품을 준비하고 있습니다.")).toBeInTheDocument();
  });
});
