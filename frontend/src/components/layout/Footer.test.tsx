import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import "../../i18n";
import { Footer } from "./Footer";

function renderFooter() {
  return render(
    <MemoryRouter>
      <Footer />
    </MemoryRouter>,
  );
}

describe("Footer", () => {
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
});
