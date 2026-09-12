import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import "../i18n";
import PrivacyPolicyPage from "./PrivacyPolicyPage";

describe("PrivacyPolicyPage", () => {
  it("제목과 본문을 렌더한다", () => {
    render(<PrivacyPolicyPage />);

    expect(screen.getByRole("heading", { name: "개인정보처리방침" })).toBeInTheDocument();
    expect(screen.getByText(/개인정보처리방침을 준비 중입니다/)).toBeInTheDocument();
  });
});
