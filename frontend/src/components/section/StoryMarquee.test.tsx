import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import "../../i18n";
import { StoryMarquee } from "./StoryMarquee";

describe("StoryMarquee", () => {
  it("브랜드 문장과 여섯 장의 카드를 보여준다", () => {
    render(<StoryMarquee />);

    expect(screen.getByRole("heading", { level: 2, name: /삶의 이야기를 식탁 위에 올리다/ })).toBeInTheDocument();
    expect(screen.getByText("사람 · 이야기 · 음식 · 예술 · 식탁 · 경험")).toBeInTheDocument();
    for (const title of [
      "사람을 만납니다",
      "삶의 이야기를 발견합니다",
      "음식으로 기억합니다",
      "예술로 만듭니다",
      "삶이 담긴 좋은 식사",
      "다시 마주 앉습니다",
    ]) {
      expect(screen.getByRole("heading", { level: 3, name: title })).toBeInTheDocument();
    }
  });

  it("끝없이 이어 보이게 붙인 두 번째 사본은 보조기기에서 숨긴다", () => {
    const { container } = render(<StoryMarquee />);

    const hiddenLists = container.querySelectorAll('ul[aria-hidden="true"]');
    expect(hiddenLists).toHaveLength(1);
    expect(within(hiddenLists[0] as HTMLElement).getAllByRole("listitem", { hidden: true })).toHaveLength(6);
    // 접근성 트리에는 사본이 빠져서 제목이 한 번씩만 잡힌다.
    expect(screen.getAllByRole("heading", { level: 3 })).toHaveLength(6);
  });

  it("일시정지 버튼으로 흐름을 멈췄다 다시 흐르게 할 수 있다", async () => {
    const user = userEvent.setup();
    render(<StoryMarquee />);

    const button = screen.getByRole("button", { name: "흐름 멈추기" });
    expect(button).toHaveAttribute("aria-pressed", "false");

    await user.click(button);

    expect(screen.getByRole("button", { name: "다시 흐르게 하기" })).toHaveAttribute("aria-pressed", "true");
  });

  it("키보드로 스크롤할 수 있는 영역으로 제공한다", () => {
    render(<StoryMarquee />);

    expect(screen.getByRole("group", { name: "파인다이닝 씨어터가 하는 일" })).toHaveAttribute("tabindex", "0");
  });
});
