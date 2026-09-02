import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import "../../i18n";
import { Footer } from "./Footer";

describe("Footer", () => {
  it("저작권 표기를 렌더한다", () => {
    render(<Footer />);

    expect(screen.getByText("© 2026 파인다이닝 씨어터")).toBeInTheDocument();
  });
});
