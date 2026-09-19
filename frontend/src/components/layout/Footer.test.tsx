import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { MemberAuthProvider } from "../../contexts/MemberAuthContext";
import { Footer } from "./Footer";

function renderFooter() {
  return render(
    <MemoryRouter>
      <MemberAuthProvider>
        <Footer />
      </MemberAuthProvider>
    </MemoryRouter>,
  );
}

const UNAUTHENTICATED = {
  json: async () => ({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } }),
} as Response;

describe("Footer", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset().mockResolvedValue(UNAUTHENTICATED);
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("로그인하지 않았으면 내 계정·회원 탈퇴 링크가 없다", async () => {
    renderFooter();
    await screen.findByRole("link", { name: "이용약관" });

    expect(screen.queryByRole("link", { name: /회원 탈퇴/ })).not.toBeInTheDocument();
  });

  it("카카오 회원으로 로그인했으면 내 계정·회원 탈퇴 링크가 있다", async () => {
    fetchMock.mockImplementation((input: string) =>
      Promise.resolve(
        input === "/api/auth/member/refresh"
          ? ({ json: async () => ({ success: true, data: { accountId: 4, accessToken: "t", nickname: "손님" }, error: null }) } as Response)
          : UNAUTHENTICATED,
      ),
    );
    renderFooter();

    expect(await screen.findByRole("link", { name: /회원 탈퇴/ })).toHaveAttribute("href", "/account");
  });

  it("저작권 표기를 렌더한다", () => {
    renderFooter();

    expect(screen.getByText("© 2026 파인다이닝 씨어터")).toBeInTheDocument();
  });

  it("사업자 정보(상호·대표자·사업자등록번호·주소·연락처·이메일)를 렌더한다", () => {
    renderFooter();

    expect(screen.getByText(/571-28-01830/)).toBeInTheDocument();
    expect(screen.getByText(/서울특별시 성북구 삼선교로 8-1/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "010-6776-1801" })).toHaveAttribute("href", "tel:01067761801");
    expect(screen.getByRole("link", { name: "finediningtheater@naver.com" })).toHaveAttribute(
      "href",
      "mailto:finediningtheater@naver.com",
    );
  });

  it("개인정보처리방침·이용약관 링크를 렌더한다", () => {
    renderFooter();

    expect(screen.getByRole("link", { name: "개인정보처리방침" })).toHaveAttribute("href", "/privacy");
    expect(screen.getByRole("link", { name: "이용약관" })).toHaveAttribute("href", "/terms");
  });

  it("유튜브·인스타그램·페이스북 링크를 새 창으로 여는 외부 링크로 렌더한다", () => {
    renderFooter();

    const youtube = screen.getByRole("link", { name: /유튜브/ });
    expect(youtube.getAttribute("href")).toContain("https://www.youtube.com/@");
    expect(screen.getByRole("link", { name: /인스타그램/ })).toHaveAttribute(
      "href",
      "https://www.instagram.com/finediningtheater/",
    );
    const facebook = screen.getByRole("link", { name: /페이스북/ });
    expect(facebook.getAttribute("href")).toContain("https://www.facebook.com/people/Finediningtheater/");
    for (const link of [youtube, facebook, screen.getByRole("link", { name: /인스타그램/ })]) {
      expect(link).toHaveAttribute("target", "_blank");
      expect(link).toHaveAttribute("rel", "noopener noreferrer");
    }
  });
});
