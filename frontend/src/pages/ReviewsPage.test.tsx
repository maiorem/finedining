import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import { MemberAuthProvider } from "../contexts/MemberAuthContext";
import ReviewsPage from "./ReviewsPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

const UNAUTHENTICATED = jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } });

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemberAuthProvider>
          <MemoryRouter>
            <ReviewsPage />
          </MemoryRouter>
        </MemberAuthProvider>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("ReviewsPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("비로그인 방문자에게 공개 리뷰 목록만 보여주고 모더레이션 토글·작성 폼은 없다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh") || input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      return Promise.resolve(
        jsonResponse({
          success: true,
          data: [{ id: 1, title: "잊지 못할 무대였습니다", accountId: 1, createdAt: "2026-01-01T00:00:00Z" }],
          error: null,
        }),
      );
    });

    renderPage();

    expect(await screen.findByRole("link", { name: /잊지 못할 무대였습니다/ })).toHaveAttribute(
      "href",
      "/reviews/1",
    );
    expect(screen.queryByRole("button", { name: "모더레이션 모드" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "리뷰 작성" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "로그인" })).toBeInTheDocument();
  });

  it("리뷰가 없으면 빈 상태 문구를 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh") || input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();

    expect(await screen.findByText("등록된 리뷰가 없습니다.")).toBeInTheDocument();
  });

  it("관리자로 로그인했으면 모더레이션 토글이 보인다", async () => {
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
      if (input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();

    expect(await screen.findByRole("button", { name: "모더레이션 모드" })).toBeInTheDocument();
  });

  it("회원으로 로그인했으면 리뷰 작성 폼을 열어 등록할 수 있다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      if (input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accountId: 4, accessToken: "t", nickname: "손님" }, error: null }),
        );
      }
      if (input === "/api/reviews" && init?.method === "POST") {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { id: 2, title: "새 리뷰", body: "새 본문", accountId: 4, status: "PUBLISHED" },
            error: null,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();

    await user.click(await screen.findByRole("button", { name: "리뷰 작성" }));
    await user.type(screen.getByLabelText("제목"), "새 리뷰");
    await user.type(screen.getByLabelText("본문"), "새 본문");
    await user.click(screen.getByRole("button", { name: "등록하기" }));

    expect(
      await screen.findByRole("button", { name: "리뷰 작성" }),
    ).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/reviews",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ title: "새 리뷰", body: "새 본문" }) }),
    );
  });
});
