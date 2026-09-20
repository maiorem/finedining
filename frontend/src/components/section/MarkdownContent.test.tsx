import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import "../../i18n";
import type { MediaAsset } from "../../api/media";
import { MarkdownContent } from "./MarkdownContent";

const image: MediaAsset = {
  id: 7,
  status: "READY",
  failureReason: null,
  width: 640,
  height: 400,
  altText: "기본 설명",
  caption: null,
  lqipBase64: null,
  url640: "http://example.com/640.jpg",
  url960: null,
  url1600: null,
  published: true,
};

describe("MarkdownContent", () => {
  it("소제목·질문·문단·목록을 렌더한다", () => {
    render(<MarkdownContent source={"# 큰 제목\n\n**질문입니다?**\n\n본문 줄\n\n- 하나\n- 둘"} />);

    expect(screen.getByRole("heading", { level: 2, name: "큰 제목" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "질문입니다?" })).toBeInTheDocument();
    expect(screen.getByText("본문 줄")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
  });

  it("image:번호를 이 글의 이미지로 바꿔 그린다. 없는 번호는 그리지 않는다", () => {
    render(<MarkdownContent source={"![현장](image:7)\n\n![없음](image:99)"} images={[image]} />);

    expect(screen.getByRole("img", { name: "현장" })).toHaveAttribute("src", "http://example.com/640.jpg");
    expect(screen.queryByRole("img", { name: "없음" })).not.toBeInTheDocument();
  });

  it("가로 크기를 지정하면 그 폭으로 그리고 세로는 원본 비율로 계산한다", () => {
    render(<MarkdownContent source="![현장](image:7|320)" images={[image]} />);

    const img = screen.getByRole("img", { name: "현장" });
    expect(img).toHaveAttribute("width", "320");
    expect(img).toHaveAttribute("height", "200"); // 640x400 → 320x200
    expect(img).toHaveStyle({ width: "320px" });
  });

  it("크기를 지정하지 않으면 원본 크기 속성 그대로 두고 스타일을 덧입히지 않는다", () => {
    render(<MarkdownContent source="![현장](image:7)" images={[image]} />);

    const img = screen.getByRole("img", { name: "현장" });
    expect(img).toHaveAttribute("width", "640");
    expect(img).not.toHaveAttribute("style");
  });

  it("HTML은 실행하지 않고 글자로 보여준다", () => {
    const { container } = render(<MarkdownContent source={"<img src=x onerror=alert(1)> <script>alert(1)</script>"} />);

    expect(container.querySelector("script")).toBeNull();
    expect(container.querySelector("img")).toBeNull();
    expect(container).toHaveTextContent("<script>alert(1)</script>");
  });

  it("외부 링크는 새 창으로, 위험한 주소는 링크로 만들지 않는다", () => {
    render(<MarkdownContent source={"[안전](https://example.com) [위험](javascript:alert(1))"} />);

    expect(screen.getByRole("link", { name: /안전/ })).toHaveAttribute("target", "_blank");
    expect(screen.getAllByRole("link")).toHaveLength(1);
  });

  it("비어 있으면 아무것도 그리지 않는다", () => {
    const { container } = render(<MarkdownContent source={null} />);

    expect(container).toBeEmptyDOMElement();
  });
});
