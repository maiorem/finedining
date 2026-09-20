import { describe, expect, it } from "vitest";
import { newImageToken, removeNewImage, resolveNewImageTokens } from "./reviewImages";

describe("reviewImages", () => {
  it("임시 표시를 만든다", () => {
    expect(newImageToken(2, "현장", 400)).toBe("![현장](image:new2|400)");
    expect(newImageToken(1, "", null)).toBe("![](image:new1)");
  });

  it("올린 사진의 실제 번호로 바꾸고 올리지 못한 사진의 줄은 지운다", () => {
    const body = "앞글\n\n![a](image:new1|300)\n\n중간\n\n![b](image:new2)\n\n뒷글";

    expect(resolveNewImageTokens(body, [11, null])).toBe("앞글\n\n![a](image:11|300)\n\n중간\n\n뒷글");
  });

  it("파일을 뺄 때 그 줄을 지우고 뒤 번호를 당긴다", () => {
    const body = "![a](image:new1)\n\n글\n\n![b](image:new2)\n\n![c](image:new3)";

    expect(removeNewImage(body, 2)).toBe("![a](image:new1)\n\n글\n\n![c](image:new2)");
  });

  it("임시 표시가 아닌 줄과 이미 실제 번호인 이미지는 건드리지 않는다", () => {
    expect(resolveNewImageTokens("글\n\n![x](image:5)", [])).toBe("글\n\n![x](image:5)");
  });
});
