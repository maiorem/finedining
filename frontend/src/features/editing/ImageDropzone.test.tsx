import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import { ImageDropzone } from "./ImageDropzone";
import type { MediaAsset } from "../../api/media";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

const image: MediaAsset = {
  id: 9,
  status: "READY",
  failureReason: null,
  width: 640,
  height: 400,
  altText: "옛 대체텍스트",
  caption: "옛 설명 문단",
  lqipBase64: null,
  url640: "http://example.com/640.jpg",
  url960: null,
  url1600: null,
  published: true,
};

function renderDropzone(onChanged: () => void) {
  const fetchMock = vi.fn<(input: string, init?: RequestInit) => Promise<Response>>((input) => {
    if (input.includes("/api/auth/admin/refresh")) {
      return Promise.resolve(
        jsonResponse({ success: true, data: { accessToken: "t", username: "admin", role: "EDITOR" }, error: null }),
      );
    }
    return Promise.resolve(jsonResponse({ success: true, data: image, error: null }));
  });
  vi.stubGlobal("fetch", fetchMock);

  render(
    <AdminAuthProvider>
      <ImageDropzone ownerType="PRODUCTION" ownerId={1} images={[image]} onChanged={onChanged} />
    </AdminAuthProvider>,
  );

  return fetchMock;
}

describe("ImageDropzone — 기존 이미지 대체텍스트·설명 수정", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("수정 버튼을 누르면 대체텍스트·설명이 채워진 폼이 뜨고, 저장하면 두 값을 각각 PUT한다", async () => {
    const user = userEvent.setup();
    const onChanged = vi.fn();
    const fetchMock = renderDropzone(onChanged);

    await user.click(await screen.findByRole("button", { name: "수정" }));

    const altInput = screen.getByLabelText("대체 텍스트") as HTMLInputElement;
    const captionInput = screen.getByLabelText("설명 문단") as HTMLTextAreaElement;
    expect(altInput.value).toBe("옛 대체텍스트");
    expect(captionInput.value).toBe("옛 설명 문단");

    await user.clear(altInput);
    await user.type(altInput, "새 대체텍스트");
    await user.clear(captionInput);
    await user.type(captionInput, "새 설명 문단");
    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => expect(onChanged).toHaveBeenCalled());

    const altCall = fetchMock.mock.calls.find((call) => call[0] === "/api/media/9" && call[1]?.method === "PUT");
    expect(JSON.parse(altCall![1]!.body as string)).toEqual({ altText: "새 대체텍스트" });

    const captionCall = fetchMock.mock.calls.find(
      (call) => call[0] === "/api/media/9/caption" && call[1]?.method === "PUT",
    );
    expect(JSON.parse(captionCall![1]!.body as string)).toEqual({ caption: "새 설명 문단" });
  });

  it("취소하면 원래 값으로 돌아가고 요청을 보내지 않는다", async () => {
    const user = userEvent.setup();
    const onChanged = vi.fn();
    const fetchMock = renderDropzone(onChanged);

    await user.click(await screen.findByRole("button", { name: "수정" }));
    await user.click(screen.getByRole("button", { name: "취소" }));

    expect(screen.getByText("옛 대체텍스트")).toBeInTheDocument();
    expect(screen.getByText("옛 설명 문단")).toBeInTheDocument();
    expect(fetchMock.mock.calls.some((call) => call[1]?.method === "PUT")).toBe(false);
    expect(onChanged).not.toHaveBeenCalled();
  });
});
