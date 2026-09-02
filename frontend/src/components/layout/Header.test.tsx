import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import "../../i18n";
import { Header } from "./Header";

function renderHeader() {
  return render(
    <MemoryRouter>
      <Header />
    </MemoryRouter>,
  );
}

describe("Header", () => {
  it("소개·작품 예매하기·프로그램·아티스트·리뷰·협업제안 내비게이션 링크를 렌더한다", () => {
    renderHeader();

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
    const user = userEvent.setup();
    renderHeader();

    const menuButton = screen.getByRole("button", { name: "메뉴 열기" });
    expect(screen.getAllByRole("link", { name: "작품 예매하기" })).toHaveLength(1);

    await user.click(menuButton);
    expect(screen.getAllByRole("link", { name: "작품 예매하기" })).toHaveLength(2);
    expect(screen.getByRole("button", { name: "메뉴 닫기" })).toBeInTheDocument();
  });
});
