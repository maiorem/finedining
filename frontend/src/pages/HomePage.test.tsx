import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import "../i18n";
import HomePage from "./HomePage";

describe("HomePage", () => {
  it("사이트 이름을 렌더한다", () => {
    render(<HomePage />);

    expect(screen.getByRole("heading", { name: "파인다이닝 씨어터" })).toBeInTheDocument();
  });
});
