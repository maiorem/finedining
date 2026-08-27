import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import LoginPage from "./LoginPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <AdminAuthProvider>
        <LoginPage />
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

  it("기본 상태에서는 관리자 폼이 숨겨져 있다", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "인증이 필요합니다." } }),
    );

    renderLoginPage();

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(screen.queryByLabelText("아이디")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "카카오로 로그인" })).toBeDisabled();
  });

  it("ADMIN 버튼을 누르면 폼이 나타나고 로그인에 성공하면 세션이 보인다", async () => {
    const user = userEvent.setup();
    fetchMock
      .mockResolvedValueOnce(
        jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: { accessToken: "token", username: "admin", role: "SUPER_ADMIN" },
          error: null,
        }),
      );

    renderLoginPage();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

    await user.click(screen.getByRole("button", { name: "ADMIN" }));
    await user.type(screen.getByLabelText("아이디"), "admin");
    await user.type(screen.getByLabelText("비밀번호"), "ChangeMe!2026");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByText("admin님으로 로그인했습니다 (SUPER_ADMIN)")).toBeInTheDocument();
  });

  it("자격증명이 틀리면 에러 메시지를 보여준다", async () => {
    const user = userEvent.setup();
    fetchMock
      .mockResolvedValueOnce(
        jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: false,
          data: null,
          error: { code: "INVALID_CREDENTIALS", message: "아이디 또는 비밀번호가 올바르지 않습니다." },
        }),
      );

    renderLoginPage();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

    await user.click(screen.getByRole("button", { name: "ADMIN" }));
    await user.type(screen.getByLabelText("아이디"), "admin");
    await user.type(screen.getByLabelText("비밀번호"), "wrong");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("아이디 또는 비밀번호가 올바르지 않습니다.");
  });
});
