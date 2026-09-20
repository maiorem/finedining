import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { MemberAuthProvider } from "../../contexts/MemberAuthContext";
import { ReviewWriteForm } from "./ReviewWriteForm";

function json(body: unknown): Response {
  return { json: async () => body, ok: true } as Response;
}

describe("ReviewWriteForm 본문 속 사진", () => {
  const fetchMock = vi.fn();
  const requests = () => fetchMock.mock.calls.map(([url, init]) => ({ url: url as string, method: (init?.method as string) ?? "GET", body: init?.body as string | undefined }));

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function stub(imageOk: boolean) {
    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input === "/api/auth/member/refresh") {
        return Promise.resolve(json({ success: true, data: { accountId: 4, accessToken: "tok", nickname: "손님" }, error: null }));
      }
      if (input === "/api/reviews" && init?.method === "POST") {
        return Promise.resolve(json({ success: true, data: { id: 9, title: "제목", body: "", accountId: 4, status: "PUBLISHED" }, error: null }));
      }
      if (input === "/api/reviews/9/images/presign") {
        return Promise.resolve(
          imageOk
            ? json({ success: true, data: { mediaAssetId: 77, uploadUrl: "http://s3/up" }, error: null })
            : json({ success: false, data: null, error: { code: "VALIDATION_ERROR", message: "x" } }),
        );
      }
      if (input === "http://s3/up") return Promise.resolve({ ok: true } as Response);
      if (input === "/api/reviews/9/images/77/complete") {
        return Promise.resolve(json({ success: true, data: { id: 77, status: "READY", url640: "http://x/640.jpg" }, error: null }));
      }
      if (input === "/api/reviews/9" && init?.method === "PUT") {
        return Promise.resolve(json({ success: true, data: { id: 9 }, error: null }));
      }
      return Promise.resolve(json({ success: false, data: null, error: { code: "X", message: "x" } }));
    });
  }

  async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>) {
    render(
      <MemoryRouter>
        <MemberAuthProvider>
          <ReviewWriteForm onCreated={onCreated} />
        </MemberAuthProvider>
      </MemoryRouter>,
    );
    await user.type(await screen.findByLabelText("제목"), "제목");
    await user.type(await screen.findByLabelText("본문"), "본문");
    await user.type(screen.getByLabelText("이름"), "홍길동");
    await user.upload(screen.getByLabelText("이미지 삽입", { selector: "input" }), new File(["x"], "a.jpg", { type: "image/jpeg" }));
    await user.click(screen.getByRole("button", { name: /본문에 넣기/ }));
    await user.click(screen.getByRole("checkbox", { name: /개인정보 수집/ }));
    await user.click(screen.getByRole("button", { name: "등록하기" }));
  }

  const onCreated = vi.fn();

  it("등록 후 사진을 올리고 본문의 임시 표시를 실제 번호로 바꿔 다시 저장한다", async () => {
    stub(true);
    onCreated.mockReset();
    await fillAndSubmit(userEvent.setup());

    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(null));
    const put = requests().find((r) => r.url === "/api/reviews/9" && r.method === "PUT");
    expect(put).toBeDefined();
    expect(JSON.parse(put!.body!).body).toContain("![](image:77)");
    expect(JSON.parse(put!.body!).body).not.toContain("image:new");
  });

  it("사진을 올리지 못하면 그 사진의 줄을 지운 본문으로 저장하고 실패를 알린다", async () => {
    stub(false);
    onCreated.mockReset();
    await fillAndSubmit(userEvent.setup());

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(onCreated.mock.calls[0][0]).toContain("올리지 못했습니다");
    const put = requests().find((r) => r.url === "/api/reviews/9" && r.method === "PUT");
    expect(JSON.parse(put!.body!).body).not.toContain("image:");
  });
});
