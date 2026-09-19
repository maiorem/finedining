import { describe, expect, it } from "vitest";
import { parseStoryBlocks } from "./storyBlocks";

describe("parseStoryBlocks", () => {
  it("빈 줄로 나눈 문단을 만들고 **로 감싼 한 줄은 질문으로 본다", () => {
    const blocks = parseStoryBlocks("**아버지는 어떤 삶을 살아왔나요?**\n\n그는 오랫동안\n주방에서 일했습니다.\n\n**지금은요?**\n\n쉬고 있습니다.");

    expect(blocks).toEqual([
      { type: "question", text: "아버지는 어떤 삶을 살아왔나요?" },
      { type: "text", text: "그는 오랫동안\n주방에서 일했습니다." },
      { type: "question", text: "지금은요?" },
      { type: "text", text: "쉬고 있습니다." },
    ]);
  });

  it("문장 중간의 **는 질문으로 바꾸지 않는다", () => {
    expect(parseStoryBlocks("이건 **중간** 강조")).toEqual([{ type: "text", text: "이건 **중간** 강조" }]);
  });

  it("비어 있으면 빈 배열", () => {
    expect(parseStoryBlocks(null)).toEqual([]);
    expect(parseStoryBlocks("  \n\n ")).toEqual([]);
  });
});
