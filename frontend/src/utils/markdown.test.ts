import { describe, expect, it } from "vitest";
import { parseInline, parseMarkdown, referencedImageIds } from "./markdown";

describe("parseMarkdown", () => {
  it("소제목·문단·목록·인용·구분선을 블록으로 나눈다", () => {
    const blocks = parseMarkdown("# 제목\n\n첫 줄\n둘째 줄\n\n- 하나\n- 둘\n\n1. 가\n2. 나\n\n> 인용\n\n---");

    expect(blocks.map((b) => b.type)).toEqual(["heading", "paragraph", "list", "list", "quote", "hr"]);
    expect(blocks[1]).toMatchObject({ type: "paragraph", lines: [[{ type: "text", text: "첫 줄" }], [{ type: "text", text: "둘째 줄" }]] });
    expect(blocks[2]).toMatchObject({ ordered: false });
    expect(blocks[3]).toMatchObject({ ordered: true });
  });

  it("한 줄 전체를 **로 감싼 문단은 질문이다(기존 표기 호환)", () => {
    expect(parseMarkdown("**아버지는 어떤 삶을 살아왔나요?**")[0]).toMatchObject({ type: "question" });
    expect(parseMarkdown("이건 **중간** 강조")[0]).toMatchObject({ type: "paragraph" });
  });

  it("이미지 줄과 유튜브 줄을 블록으로 만든다", () => {
    const blocks = parseMarkdown("![현장 사진](image:12)\n\n@[youtube](https://youtu.be/abc123XYZ_-)");

    expect(blocks[0]).toEqual({ type: "image", id: 12, alt: "현장 사진" });
    expect(blocks[1]).toEqual({ type: "youtube", id: "abc123XYZ_-" });
  });

  it("이미지 뒤에 |가로픽셀을 붙이면 그 폭을 읽고 범위를 벗어난 값은 잘라 낸다", () => {
    expect(parseMarkdown("![a](image:5|400)")[0]).toEqual({ type: "image", id: 5, alt: "a", width: 400 });
    expect(parseMarkdown("![a](image:5|9999)")[0]).toMatchObject({ width: 1600 });
    expect(parseMarkdown("![a](image:5|10)")[0]).toMatchObject({ width: 50 });
    expect(parseMarkdown("![a](image:5)")[0]).not.toHaveProperty("width");
  });

  it("외부 주소 이미지와 유튜브가 아닌 주소는 이미지·영상이 되지 않는다", () => {
    const blocks = parseMarkdown("![x](https://evil.example.com/a.png)\n\n@[youtube](https://vimeo.com/1)");

    expect(blocks.some((b) => b.type === "image" || b.type === "youtube")).toBe(false);
  });

  it("옵션으로 이미지·유튜브를 끄면 글자로만 남는다", () => {
    const blocks = parseMarkdown("![a](image:1)\n\n@[youtube](https://youtu.be/abc123XYZ_-)", {
      images: false,
      youtube: false,
    });

    expect(blocks.every((b) => b.type === "paragraph")).toBe(true);
  });

  it("HTML 태그는 해석하지 않고 글자로 둔다", () => {
    const blocks = parseMarkdown("<script>alert(1)</script>");

    expect(blocks[0]).toMatchObject({ type: "paragraph", lines: [[{ type: "text", text: "<script>alert(1)</script>" }]] });
  });

  it("본문이 참조한 이미지 번호를 모은다", () => {
    expect([...referencedImageIds("![a](image:3)\n\n글\n\n![b](image:9)")]).toEqual([3, 9]);
  });

  it("비어 있으면 빈 배열", () => {
    expect(parseMarkdown(null)).toEqual([]);
    expect(parseMarkdown("  \n\n ")).toEqual([]);
  });
});

describe("parseInline", () => {
  it("굵게·기울임·링크를 해석한다", () => {
    const nodes = parseInline("가 **굵게** 와 *기울임* 그리고 [링크](https://example.com)");

    expect(nodes.map((n) => n.type)).toEqual(["text", "strong", "text", "em", "text", "link"]);
    expect(nodes[5]).toMatchObject({ type: "link", href: "https://example.com" });
  });

  it("javascript: 같은 위험한 주소는 링크로 만들지 않고 글자만 남긴다", () => {
    const nodes = parseInline("[클릭](javascript:alert(1))");

    expect(nodes.some((n) => n.type === "link")).toBe(false);
  });

  it("링크를 끄면 라벨만 남는다", () => {
    const nodes = parseInline("[라벨](https://example.com)", false);

    expect(nodes).toEqual([{ type: "text", text: "라벨" }]);
  });

  it("별표 한 개만 있으면 강조로 바꾸지 않는다", () => {
    expect(parseInline("가격은 5 * 3")).toEqual([{ type: "text", text: "가격은 5 * 3" }]);
  });
});
