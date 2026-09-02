import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import HomePage from "./HomePage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderHome() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("HomePage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("사이트 이름과 히어로 한가운데 예매하기 버튼, 협업 제안하기 버튼을 렌더한다", async () => {
    fetchMock.mockResolvedValue(jsonResponse({ success: true, data: [], error: null }));

    renderHome();

    expect(screen.getByRole("heading", { name: "파인다이닝 씨어터" })).toBeInTheDocument();

    const bookingLink = screen.getByRole("link", { name: "예매하기" });
    expect(bookingLink).toHaveAttribute("href", "/productions");

    const collaborateLink = screen.getByRole("link", { name: "협업 제안하기" });
    expect(collaborateLink).toHaveAttribute("href", "/proposal");
  });

  it("작품·프로그램을 각각 최대 2개까지 보여주고 각 상세 페이지로 연결한다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.startsWith("/api/productions")) {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: [
              { id: 1, slug: "showcase-1", title: "쇼케이스 1", bookingUrl: null, locationUrl: null, thumbnail: null },
              { id: 2, slug: "showcase-2", title: "쇼케이스 2", bookingUrl: null, locationUrl: null, thumbnail: null },
              { id: 3, slug: "showcase-3", title: "쇼케이스 3", bookingUrl: null, locationUrl: null, thumbnail: null },
            ],
            error: null,
          }),
        );
      }
      if (input.startsWith("/api/programs")) {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: [
              {
                id: 10,
                slug: "program-1",
                title: "프로그램 1",
                description: null,
                applyUrl: null,
                locationUrl: null,
                thumbnail: null,
              },
              {
                id: 11,
                slug: "program-2",
                title: "프로그램 2",
                description: null,
                applyUrl: null,
                locationUrl: null,
                thumbnail: null,
              },
            ],
            error: null,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderHome();

    const showcase1 = await screen.findByRole("link", { name: /쇼케이스 1/ });
    expect(showcase1).toHaveAttribute("href", "/productions/showcase-1");
    const showcase2 = screen.getByRole("link", { name: /쇼케이스 2/ });
    expect(showcase2).toHaveAttribute("href", "/productions/showcase-2");
    expect(screen.queryByRole("link", { name: /쇼케이스 3/ })).not.toBeInTheDocument();

    const program1 = screen.getByRole("link", { name: /프로그램 1/ });
    expect(program1).toHaveAttribute("href", "/programs/program-1");
    const program2 = screen.getByRole("link", { name: /프로그램 2/ });
    expect(program2).toHaveAttribute("href", "/programs/program-2");
  });
});
