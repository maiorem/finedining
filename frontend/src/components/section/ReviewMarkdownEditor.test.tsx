import { useState } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import "../../i18n";
import ReviewMarkdownEditor, { type ReviewImageMode } from "./ReviewMarkdownEditor";

function Harness({ images, initial = "" }: { images: ReviewImageMode; initial?: string }) {
  const [value, setValue] = useState(initial);
  return (
    <>
      <ReviewMarkdownEditor id="b" label="본문" value={value} onChange={setValue} images={images} />
      <output data-testid="value">{value}</output>
    </>
  );
}

const jpeg = (name = "a.jpg") => new File(["x"], name, { type: "image/jpeg" });

describe("ReviewMarkdownEditor (회원용)", () => {
  it("서식 버튼만 있고 링크·유튜브·구분선 버튼은 없다", () => {
    render(<Harness images={{ kind: "none" }} />);

    for (const name of ["굵게", "기울임", "소제목", "목록", "인용"]) {
      expect(screen.getByRole("button", { name })).toBeInTheDocument();
    }
    for (const name of ["링크", "유튜브", "구분선", "이미지 삽입"]) {
      expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
    }
  });

  it("새 글에서는 사진을 첨부 목록에 넘기고 본문에는 임시 표시를 넣는다", async () => {
    const user = userEvent.setup();
    const onAttach = vi.fn();
    render(<Harness initial="앞글뒷글" images={{ kind: "deferred", fileCount: 0, onAttach }} />);
    (screen.getByRole("textbox", { name: "본문" }) as HTMLTextAreaElement).setSelectionRange(2, 2);

    await user.upload(screen.getByLabelText("이미지 삽입", { selector: "input" }), jpeg());
    await user.type(screen.getByLabelText(/사진 설명/), "현장");
    await user.type(screen.getByLabelText(/가로 크기/), "300");
    await user.click(screen.getByRole("button", { name: /본문에 넣기/ }));

    expect(onAttach).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("value")).toHaveTextContent("앞글 ![현장](image:new1|300) 뒷글");
  });

  it("이미 있는 글에서는 바로 올리고 실제 번호를 본문에 넣는다", async () => {
    const user = userEvent.setup();
    const onUpload = vi.fn().mockResolvedValue(77);
    render(<Harness images={{ kind: "immediate", imageCount: 1, onUpload }} />);

    await user.upload(screen.getByLabelText("이미지 삽입", { selector: "input" }), jpeg());
    await user.click(screen.getByRole("button", { name: "올리고 본문에 넣기" }));

    expect(onUpload).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.getByTestId("value")).toHaveTextContent("![](image:77)"));
  });

  it("이미 3장이면 더 붙이지 못한다", async () => {
    const user = userEvent.setup();
    render(<Harness images={{ kind: "deferred", fileCount: 3, onAttach: vi.fn() }} />);

    await user.upload(screen.getByLabelText("이미지 삽입", { selector: "input" }), jpeg());

    expect(await screen.findByRole("alert")).toHaveTextContent("최대 3장");
  });

  it("10MB를 넘거나 이미지가 아닌 파일은 거부한다", async () => {
    const user = userEvent.setup({ applyAccept: false });
    render(<Harness images={{ kind: "deferred", fileCount: 0, onAttach: vi.fn() }} />);
    const input = screen.getByLabelText("이미지 삽입", { selector: "input" });

    await user.upload(input, new File(["x"], "a.pdf", { type: "application/pdf" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("JPG, PNG, WebP");

    const big = new File(["x"], "big.jpg", { type: "image/jpeg" });
    Object.defineProperty(big, "size", { value: 11 * 1024 * 1024 });
    await user.upload(input, big);
    expect(await screen.findByRole("alert")).toHaveTextContent("10MB");
  });
});
