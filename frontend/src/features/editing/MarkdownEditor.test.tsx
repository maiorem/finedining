import { useState } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import MarkdownEditor from "./MarkdownEditor";

function json(body: unknown): Response {
  return { json: async () => body, ok: true } as Response;
}

function Harness({ initial = "", onImagesChanged = () => {} }: { initial?: string; onImagesChanged?: () => void }) {
  const [value, setValue] = useState(initial);
  return (
    <AdminAuthProvider>
      <MarkdownEditor
        id="t"
        label="설명"
        value={value}
        onChange={setValue}
        ownerType="PROGRAM"
        ownerId={5}
        onImagesChanged={onImagesChanged}
        maxLength={4000}
      />
      <output data-testid="value">{value}</output>
    </AdminAuthProvider>
  );
}

describe("MarkdownEditor", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
    fetchMock.mockImplementation((input: string) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(json({ success: true, data: { accessToken: "tok", username: "a", role: "EDITOR" }, error: null }));
      }
      if (input === "/api/media/presign") {
        return Promise.resolve(json({ success: true, data: { mediaAssetId: 42, uploadUrl: "http://s3/upload" }, error: null }));
      }
      if (input === "http://s3/upload") return Promise.resolve({ ok: true } as Response);
      if (input === "/api/media/42/complete") {
        return Promise.resolve(
          json({
            success: true,
            data: { id: 42, status: "READY", failureReason: null, width: 10, height: 10, altText: "현장", caption: null, lqipBase64: null, url640: "http://x/640.jpg", url960: null, url1600: null, published: false },
            error: null,
          }),
        );
      }
      return Promise.resolve(json({ success: false, data: null, error: { code: "X", message: "x" } }));
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("굵게 버튼은 선택한 글을 **로 감싼다", async () => {
    const user = userEvent.setup();
    render(<Harness initial="가나다" />);
    const textarea = screen.getByRole("textbox", { name: "설명" }) as HTMLTextAreaElement;
    textarea.setSelectionRange(1, 2);

    await user.click(screen.getByRole("button", { name: "굵게" }));

    expect(screen.getByTestId("value")).toHaveTextContent("가**나**다");
  });

  it("이미지는 설명을 입력해야 올릴 수 있고, 올리면 본문에 image:번호로 들어간다", async () => {
    const user = userEvent.setup();
    const onImagesChanged = vi.fn();
    render(<Harness onImagesChanged={onImagesChanged} />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/auth/admin/refresh", expect.anything()));

    const file = new File(["x"], "scene.jpg", { type: "image/jpeg" });
    await user.upload(screen.getByLabelText("이미지 삽입", { selector: "input" }), file);

    await user.click(screen.getByRole("button", { name: "올리고 본문에 넣기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("이미지 설명을 입력해 주세요");

    await user.type(screen.getByLabelText(/이미지 설명/), "현장");
    await user.click(screen.getByRole("button", { name: "올리고 본문에 넣기" }));

    await waitFor(() => expect(screen.getByTestId("value")).toHaveTextContent("![현장](image:42)"));
    expect(onImagesChanged).toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledWith("/api/media/presign", expect.objectContaining({ method: "POST" }));
  });

  it("이미지는 커서가 있던 글 중간에 들어간다", async () => {
    const user = userEvent.setup();
    render(<Harness initial="앞글뒷글" />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/auth/admin/refresh", expect.anything()));
    const textarea = screen.getByRole("textbox", { name: "설명" }) as HTMLTextAreaElement;
    textarea.setSelectionRange(2, 2); // "앞글" 다음에 커서

    await user.upload(screen.getByLabelText("이미지 삽입", { selector: "input" }), new File(["x"], "scene.jpg", { type: "image/jpeg" }));
    await user.type(screen.getByLabelText(/이미지 설명/), "현장");
    await user.click(screen.getByRole("button", { name: "올리고 본문에 넣기" }));

    await waitFor(() =>
      expect((screen.getByRole("textbox", { name: "설명" }) as HTMLTextAreaElement).value).toBe("앞글\n\n![현장](image:42)\n\n뒷글"),
    );
  });

  it("가로 크기를 적으면 |픽셀이 붙고, 범위 밖 값은 거부한다", async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/auth/admin/refresh", expect.anything()));

    await user.upload(screen.getByLabelText("이미지 삽입", { selector: "input" }), new File(["x"], "scene.jpg", { type: "image/jpeg" }));
    await user.type(screen.getByLabelText(/이미지 설명/), "현장");
    await user.type(screen.getByLabelText(/가로 크기/), "20");
    await user.click(screen.getByRole("button", { name: "올리고 본문에 넣기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("50~1600");
    expect(fetchMock).not.toHaveBeenCalledWith("/api/media/presign", expect.anything());

    await user.clear(screen.getByLabelText(/가로 크기/));
    await user.type(screen.getByLabelText(/가로 크기/), "400");
    await user.click(screen.getByRole("button", { name: "올리고 본문에 넣기" }));

    await waitFor(() => expect(screen.getByTestId("value")).toHaveTextContent("![현장](image:42|400)"));
  });

  it("이미지가 아닌 파일은 거부한다", async () => {
    const user = userEvent.setup({ applyAccept: false });
    render(<Harness />);

    await user.upload(screen.getByLabelText("이미지 삽입", { selector: "input" }), new File(["x"], "a.pdf", { type: "application/pdf" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("JPG, PNG, WebP");
    expect(fetchMock).not.toHaveBeenCalledWith("/api/media/presign", expect.anything());
  });

  it("유튜브가 아닌 주소는 넣지 않는다", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "prompt").mockReturnValue("https://vimeo.com/1");
    render(<Harness />);

    await user.click(screen.getByRole("button", { name: "유튜브" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("유튜브 주소가 아닙니다");
    expect(screen.getByTestId("value")).toHaveTextContent("");
  });
});
