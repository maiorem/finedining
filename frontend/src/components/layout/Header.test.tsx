import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import { MemberAuthProvider } from "../../contexts/MemberAuthContext";
import { Header } from "./Header";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderHeader() {
  return render(
    <MemoryRouter>
      <AdminAuthProvider>
        <MemberAuthProvider>
          <Header />
        </MemberAuthProvider>
      </AdminAuthProvider>
    </MemoryRouter>,
  );
}

const UNAUTHENTICATED = jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } });

describe("Header", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("소개·작품 예매하기·프로그램·아티스트·리뷰·협업제안 내비게이션 링크를 렌더한다", async () => {
    fetchMock.mockResolvedValue(UNAUTHENTICATED);
    renderHeader();
    await screen.findByRole("link", { name: "로그인" }); // 초기 세션 복구(admin/member refresh)가 끝나길 기다린다

    expect(screen.getAllByRole("link", { name: "소개" })[0]).toHaveAttribute("href", "/about");
    expect(screen.getAllByRole("link", { name: "작품 예매하기" })[0]).toHaveAttribute(
      "href",
      "/productions",
    );
    expect(screen.getAllByRole("link", { name: "프로그램" })[0]).toHaveAttribute(
      "href",
      "/programs",
    );
    expect(screen.getAllByRole("link", { name: "아티스트" })[0]).toHaveAttribute(
      "href",
      "/artists",
    );
    expect(screen.getAllByRole("link", { name: "리뷰" })[0]).toHaveAttribute("href", "/reviews");
    expect(screen.getAllByRole("link", { name: "협업제안" })[0]).toHaveAttribute(
      "href",
      "/proposal",
    );
  });

  it("모바일 메뉴 버튼을 누르면 패널이 열리고 닫힌다", async () => {
    fetchMock.mockResolvedValue(UNAUTHENTICATED);
    const user = userEvent.setup();
    renderHeader();
    await screen.findByRole("link", { name: "로그인" });

    const menuButton = screen.getByRole("button", { name: "메뉴 열기" });
    expect(screen.getAllByRole("link", { name: "작품 예매하기" })).toHaveLength(1);

    await user.click(menuButton);
    expect(screen.getAllByRole("link", { name: "작품 예매하기" })).toHaveLength(2);
    expect(screen.getByRole("button", { name: "메뉴 닫기" })).toBeInTheDocument();
  });

  // 로그인 진입점이 푸터에 숨어 있어 안 보인다는 피드백으로 헤더에 항상 보이는 아이콘으로 옮겼다.
  it("로그인 상태가 아니면 헤더에 /login 링크를 보여준다", async () => {
    fetchMock.mockResolvedValue(UNAUTHENTICATED);
    renderHeader();

    expect(await screen.findByRole("link", { name: "로그인" })).toHaveAttribute("href", "/login");
  });

  it("관리자로 로그인된 상태면 로그아웃 버튼을 보여주고 누르면 로그아웃한다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { accessToken: "token", username: "admin", role: "SUPER_ADMIN" },
            error: null,
          }),
        );
      }
      if (input === "/api/auth/admin/logout") {
        return Promise.resolve(jsonResponse({ success: true, data: null, error: null }));
      }
      return Promise.resolve(UNAUTHENTICATED);
    });

    renderHeader();

    const logoutButton = await screen.findByRole("button", { name: "관리자 로그아웃" });
    expect(screen.queryByRole("link", { name: "로그인" })).not.toBeInTheDocument();

    await user.click(logoutButton);

    expect(await screen.findByRole("link", { name: "로그인" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/auth/admin/logout", expect.objectContaining({ method: "POST" }));
  });

  it("카카오로 로그인된 일반 회원이면 로그아웃 버튼을 보여주고 누르면 로그아웃한다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string) => {
      if (input === "/api/auth/member/refresh") {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accessToken: "token", nickname: "손님" }, error: null }),
        );
      }
      if (input === "/api/auth/member/logout") {
        return Promise.resolve(jsonResponse({ success: true, data: null, error: null }));
      }
      return Promise.resolve(UNAUTHENTICATED);
    });

    renderHeader();

    const logoutButton = await screen.findByRole("button", { name: "로그아웃" });
    expect(screen.queryByRole("link", { name: "로그인" })).not.toBeInTheDocument();

    await user.click(logoutButton);

    expect(await screen.findByRole("link", { name: "로그인" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/auth/member/logout", expect.objectContaining({ method: "POST" }));
  });
});
