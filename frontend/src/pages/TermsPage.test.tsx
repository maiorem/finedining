import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import "../i18n";
import TermsPage from "./TermsPage";

describe("TermsPage", () => {
  it("제목과 본문을 렌더한다", () => {
    render(<TermsPage />);

    expect(screen.getByRole("heading", { name: "이용약관" })).toBeInTheDocument();
    expect(screen.getByText(/이용약관을 준비 중입니다/)).toBeInTheDocument();
  });
});
