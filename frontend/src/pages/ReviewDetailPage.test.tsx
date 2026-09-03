import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import { MemberAuthProvider } from "../contexts/MemberAuthContext";
import ReviewDetailPage from "./ReviewDetailPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

const UNAUTHENTICATED = jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } });

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemberAuthProvider>
          <MemoryRouter initialEntries={[path]}>
            <Routes>
              <Route path="/reviews/:id" element={<ReviewDetailPage />} />
              <Route path="/reviews" element={<p>reviews-list</p>} />
            </Routes>
          </MemoryRouter>
        </MemberAuthProvider>
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

const REVIEW_DATA = {
  id: 1,
  title: "잊지 못할 무대였습니다",
  body: "본문 내용",
  accountId: 4,
  createdAt: "2026-01-01T00:00:00Z",
  comments: [{ id: 10, accountId: 4, body: "저도 좋았어요", createdAt: "2026-01-02T00:00:00Z" }],
};

describe("ReviewDetailPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("비로그인 방문자에게 제목·본문·댓글을 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh") || input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      return Promise.resolve(jsonResponse({ success: true, data: REVIEW_DATA, error: null }));
    });

    renderAt("/reviews/1");

    expect(await screen.findByRole("heading", { name: "잊지 못할 무대였습니다" })).toBeInTheDocument();
    expect(screen.getByText("본문 내용")).toBeInTheDocument();
    expect(screen.getByText("저도 좋았어요")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "수정" })).not.toBeInTheDocument();
  });

  it("존재하지 않는 리뷰면 안내 문구를 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh") || input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      return Promise.resolve(
        jsonResponse({ success: false, data: null, error: { code: "ENTITY_NOT_FOUND", message: "x" } }),
      );
    });

    renderAt("/reviews/99");

    expect(await screen.findByText("존재하지 않는 리뷰입니다.")).toBeInTheDocument();
  });

  it("관리자가 모더레이션 모드를 켜면 원문 수정 폼과 댓글 삭제 버튼이 보인다", async () => {
    const user = userEvent.setup();
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
      if (input.includes("/api/reviews/manage/")) {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: {
              id: 1,
              title: "잊지 못할 무대였습니다",
              body: "본문 내용",
              accountId: 1,
              status: "PUBLISHED",
              createdAt: "2026-01-01T00:00:00Z",
              updatedAt: "2026-01-01T00:00:00Z",
              comments: [{ id: 10, accountId: 4, body: "저도 좋았어요", createdAt: "2026-01-02T00:00:00Z" }],
            },
            error: null,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: REVIEW_DATA, error: null }));
    });

    renderAt("/reviews/1");

    await user.click(await screen.findByRole("button", { name: "모더레이션 모드" }));

    expect(await screen.findByDisplayValue("잊지 못할 무대였습니다")).toBeInTheDocument();
    expect(screen.getByDisplayValue("본문 내용")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "삭제" })).toBeInTheDocument();
  });

  it("본인 글이면 수정·삭제 버튼이 보이고, 다른 회원 글이면 보이지 않는다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      if (input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accountId: 999, accessToken: "t", nickname: "다른회원" }, error: null }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: REVIEW_DATA, error: null }));
    });

    renderAt("/reviews/1");

    await screen.findByRole("heading", { name: "잊지 못할 무대였습니다" });
    expect(screen.queryByRole("button", { name: "수정" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });

  it("본인 글이면 수정할 수 있다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      if (input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accountId: 4, accessToken: "t", nickname: "작성자" }, error: null }),
        );
      }
      if (input === "/api/reviews/1" && init?.method === "PUT") {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { id: 1, title: "고친 제목", body: "고친 본문", accountId: 4, status: "PUBLISHED" },
            error: null,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: REVIEW_DATA, error: null }));
    });

    renderAt("/reviews/1");

    await user.click(await screen.findByRole("button", { name: "수정" }));
    const titleInput = screen.getByLabelText("제목");
    await user.clear(titleInput);
    await user.type(titleInput, "고친 제목");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("button", { name: "수정" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/reviews/1",
      expect.objectContaining({ method: "PUT" }),
    );
  });

  it("본인 글을 삭제하면 목록으로 돌아간다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(UNAUTHENTICATED);
      }
      if (input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accountId: 4, accessToken: "t", nickname: "작성자" }, error: null }),
        );
      }
      if (input === "/api/reviews/1" && init?.method === "DELETE") {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { id: 1, title: "잊지 못할 무대였습니다", body: "본문 내용", accountId: 4, status: "DELETED" },
            error: null,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: REVIEW_DATA, error: null }));
    });

    renderAt("/reviews/1");

    await user.click(await screen.findByRole("button", { name: "삭제" }));

    expect(await screen.findByText("reviews-list")).toBeInTheDocument();
  });
});
