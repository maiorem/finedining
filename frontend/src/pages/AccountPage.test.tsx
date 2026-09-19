import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { MemberAuthProvider } from "../contexts/MemberAuthContext";
import AccountPage from "./AccountPage";

function json(body: unknown): Response {
  return { json: async () => body } as Response;
}

const UNAUTHENTICATED = json({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } });
const MEMBER = json({ success: true, data: { accountId: 4, accessToken: "t", nickname: "손님" }, error: null });

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemberAuthProvider>
        <MemoryRouter initialEntries={["/account"]}>
          <Routes>
            <Route path="/account" element={<AccountPage />} />
            <Route path="/" element={<p>홈</p>} />
          </Routes>
        </MemoryRouter>
      </MemberAuthProvider>
    </QueryClientProvider>,
  );
}

describe("AccountPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("로그인하지 않았으면 로그인 안내를 보여준다", async () => {
    fetchMock.mockResolvedValue(UNAUTHENTICATED);
    renderPage();

    expect(await screen.findByText(/로그인한 뒤에 이용할 수 있습니다/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "회원 탈퇴하기" })).not.toBeInTheDocument();
  });

  it("확인란을 체크해야 탈퇴할 수 있고, 탈퇴하면 DELETE 호출 뒤 홈으로 이동한다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input === "/api/auth/member/refresh") return Promise.resolve(MEMBER);
      if (input === "/api/auth/member/me" && init?.method === "DELETE") {
        return Promise.resolve(json({ success: true, data: null, error: null }));
      }
      return Promise.resolve(json({ success: true, data: null, error: null }));
    });
    renderPage();

    const submit = await screen.findByRole("button", { name: "회원 탈퇴하기" });
    expect(submit).toBeDisabled();
    await user.click(screen.getByRole("checkbox"));
    await user.click(submit);

    expect(await screen.findByText("홈")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/auth/member/me",
      expect.objectContaining({ method: "DELETE" }),
    );
  });

  it("탈퇴에 실패하면 안내를 보여주고 페이지에 남는다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input === "/api/auth/member/refresh") return Promise.resolve(MEMBER);
      if (input === "/api/auth/member/me" && init?.method === "DELETE") {
        return Promise.resolve(json({ success: false, data: null, error: { code: "INTERNAL_ERROR", message: "x" } }));
      }
      return Promise.resolve(UNAUTHENTICATED);
    });
    renderPage();

    await user.click(await screen.findByRole("checkbox"));
    await user.click(screen.getByRole("button", { name: "회원 탈퇴하기" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("탈퇴를 처리하지 못했습니다");
  });
});
