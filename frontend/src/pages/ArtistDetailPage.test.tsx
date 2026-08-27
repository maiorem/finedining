import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import ArtistDetailPage from "./ArtistDetailPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/artists/:slug" element={<ArtistDetailPage />} />
        </Routes>
      </MemoryRouter>
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

  it("아티스트 이름·소개·참여작품을 보여준다", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        success: true,
        data: {
          id: 1,
          slug: "kim-artist",
          name: "김아무개",
          role: "연출",
          bio: "소개 문구",
          linkUrl: "https://instagram.com/kimartist",
          productions: [{ id: 1, slug: "showcase", title: "쇼케이스" }],
        },
        error: null,
      }),
    );

    renderAt("/artists/kim-artist");

    expect(await screen.findByRole("heading", { name: "김아무개" })).toBeInTheDocument();
    expect(screen.getByText("소개 문구")).toBeInTheDocument();
    expect(screen.getByText("쇼케이스")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /새 창에서 열림/ })).toHaveAttribute("target", "_blank");
  });

  it("존재하지 않는 아티스트면 안내 문구를 보여준다", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        success: false,
        data: null,
        error: { code: "ENTITY_NOT_FOUND", message: "x" },
      }),
    );

    renderAt("/artists/unknown");

    expect(await screen.findByText("존재하지 않는 아티스트입니다.")).toBeInTheDocument();
  });
});
