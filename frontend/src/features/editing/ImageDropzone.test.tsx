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
  altText: "옛 캡션",
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
    return Promise.resolve(
      jsonResponse({ success: true, data: { ...image, altText: "새 캡션" }, error: null }),
    );
  });
  vi.stubGlobal("fetch", fetchMock);

  render(
    <AdminAuthProvider>
      <ImageDropzone ownerType="PRODUCTION" ownerId={1} images={[image]} onChanged={onChanged} />
    </AdminAuthProvider>,
  );

  return fetchMock;
}

describe("ImageDropzone — 기존 이미지 캡션 수정", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("캡션 수정 버튼을 누르면 현재 캡션이 채워진 입력창이 뜨고, 저장하면 PUT 요청을 보낸다", async () => {
    const user = userEvent.setup();
    const onChanged = vi.fn();
    const fetchMock = renderDropzone(onChanged);

    await user.click(await screen.findByRole("button", { name: "캡션 수정" }));

    const input = screen.getByLabelText("대체 텍스트") as HTMLInputElement;
    expect(input.value).toBe("옛 캡션");

    await user.clear(input);
    await user.type(input, "새 캡션");
    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => expect(onChanged).toHaveBeenCalled());
    const putCall = fetchMock.mock.calls.find((call) => call[1]?.method === "PUT");
    expect(putCall?.[0]).toBe("/api/media/9");
    expect(JSON.parse(putCall![1]!.body as string)).toEqual({ altText: "새 캡션" });
  });

  it("취소하면 원래 캡션 텍스트로 돌아가고 요청을 보내지 않는다", async () => {
    const user = userEvent.setup();
    const onChanged = vi.fn();
    const fetchMock = renderDropzone(onChanged);

    await user.click(await screen.findByRole("button", { name: "캡션 수정" }));
    await user.click(screen.getByRole("button", { name: "취소" }));

    expect(screen.getByText("옛 캡션")).toBeInTheDocument();
    expect(fetchMock.mock.calls.some((call) => call[1]?.method === "PUT")).toBe(false);
    expect(onChanged).not.toHaveBeenCalled();
  });
});
