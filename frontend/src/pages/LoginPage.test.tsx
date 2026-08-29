import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import { MemberAuthProvider } from "../contexts/MemberAuthContext";
import LoginPage from "./LoginPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function unauthenticated(): Response {
  return jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } });
}

function renderLoginPage(initialPath = "/login") {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AdminAuthProvider>
        <MemberAuthProvider>
          <LoginPage />
        </MemberAuthProvider>
      </AdminAuthProvider>
    </MemoryRouter>,
  );
}

describe("LoginPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("기본 상태에서는 관리자 폼이 숨겨져 있고 카카오 로그인 링크가 보인다", async () => {
    fetchMock.mockImplementation(() => Promise.resolve(unauthenticated()));

    renderLoginPage();

    expect(screen.queryByLabelText("아이디")).not.toBeInTheDocument();
    expect(await screen.findByRole("link", { name: "카카오로 로그인" })).toHaveAttribute(
      "href",
      "/api/oauth2/authorization/kakao",
    );
  });

  it("ADMIN 버튼을 누르면 폼이 나타나고 로그인에 성공하면 세션이 보인다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/login")) {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { accessToken: "token", username: "admin", role: "SUPER_ADMIN" },
            error: null,
          }),
        );
      }
      return Promise.resolve(unauthenticated());
    });

    renderLoginPage();
    await screen.findByRole("link", { name: "카카오로 로그인" });

    await user.click(screen.getByRole("button", { name: "ADMIN" }));
    await user.type(screen.getByLabelText("아이디"), "admin");
    await user.type(screen.getByLabelText("비밀번호"), "ChangeMe!2026");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByText("admin님으로 로그인했습니다 (SUPER_ADMIN)")).toBeInTheDocument();
  });

  it("자격증명이 틀리면 에러 메시지를 보여준다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/login")) {
        return Promise.resolve(
          jsonResponse({
            success: false,
            data: null,
            error: { code: "INVALID_CREDENTIALS", message: "아이디 또는 비밀번호가 올바르지 않습니다." },
          }),
        );
      }
      return Promise.resolve(unauthenticated());
    });

    renderLoginPage();
    await screen.findByRole("link", { name: "카카오로 로그인" });

    await user.click(screen.getByRole("button", { name: "ADMIN" }));
    await user.type(screen.getByLabelText("아이디"), "admin");
    await user.type(screen.getByLabelText("비밀번호"), "wrong");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("아이디 또는 비밀번호가 올바르지 않습니다.");
  });

  it("회원으로 로그인되어 있으면 닉네임과 로그아웃 버튼을 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/member/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accessToken: "t", nickname: "김아무개" }, error: null }),
        );
      }
      return Promise.resolve(unauthenticated());
    });

    renderLoginPage();

    expect(await screen.findByText("김아무개님으로 로그인했습니다.")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "카카오로 로그인" })).not.toBeInTheDocument();
  });

  it("카카오 로그인 실패로 리다이렉트되면 에러 메시지를 보여준다", async () => {
    fetchMock.mockImplementation(() => Promise.resolve(unauthenticated()));

    renderLoginPage("/login?error=SIGNUP_NOT_ALLOWED");

    expect(await screen.findByRole("alert")).toHaveTextContent("지금은 초대받은 사용자만 가입할 수 있습니다.");
  });
});
