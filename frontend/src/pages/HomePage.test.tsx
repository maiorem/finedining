import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import "../i18n";
import HomePage from "./HomePage";

describe("HomePage", () => {
  it("사이트 이름과 히어로 한가운데 예매하기 버튼을 렌더한다", () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "파인다이닝 씨어터" })).toBeInTheDocument();

    const bookingLink = screen.getByRole("link", { name: "예매하기" });
    expect(bookingLink).toHaveAttribute("href", "/productions");
  });
});
