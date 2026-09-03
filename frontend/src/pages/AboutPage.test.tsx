import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import "../i18n";
import AboutPage from "./AboutPage";

describe("AboutPage", () => {
  it("정적 소개문을 보여준다", () => {
    render(<AboutPage />);

    expect(screen.getByText(/당신의 식탁 위에 이야기를 올립니다\./)).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "소개" })).toBeInTheDocument();
  });
});
