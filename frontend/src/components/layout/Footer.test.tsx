import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import { Footer } from "./Footer";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function renderFooter() {
  return render(
    <MemoryRouter>
      <AdminAuthProvider>
        <Footer />
      </AdminAuthProvider>
    </MemoryRouter>,
  );
}

describe("Footer", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("로그인 상태가 아니면 /login 링크를 보여준다", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
    );

    renderFooter();

    expect(await screen.findByRole("link", { name: "로그인" })).toHaveAttribute("href", "/login");
  });

  it("관리자로 로그인된 상태면 ADMIN LOGOUT 버튼을 보여주고 누르면 로그아웃한다", async () => {
    const user = userEvent.setup();
    fetchMock
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: { accessToken: "token", username: "admin", role: "SUPER_ADMIN" },
          error: null,
        }),
      )
      .mockResolvedValueOnce(jsonResponse({ success: true, data: null, error: null }));

    renderFooter();

    const logoutButton = await screen.findByRole("button", { name: "ADMIN LOGOUT" });
    expect(screen.queryByRole("link", { name: "로그인" })).not.toBeInTheDocument();

    await user.click(logoutButton);

    expect(await screen.findByRole("link", { name: "로그인" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenLastCalledWith("/api/auth/admin/logout", expect.objectContaining({ method: "POST" }));
  });
});
