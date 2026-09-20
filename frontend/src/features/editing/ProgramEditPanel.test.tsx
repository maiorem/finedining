import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import ProgramEditPanel from "./ProgramEditPanel";

function json(body: unknown): Response {
  return { json: async () => body, ok: true } as Response;
}

const ADMIN_DATA = {
  id: 1,
  slug: "sample",
  status: "PUBLISHED",
  applyUrl: null,
  locationUrl: null,
  translations: [
    {
      locale: "KO",
      title: "여름 시식회",
      subtitle: null,
      description: "저장된 설명",
      draftTitle: null,
      draftSubtitle: null,
      draftDescription: null,
      hasPendingDraft: false,
    },
  ],
  images: [],
};

describe("ProgramEditPanel", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
    fetchMock.mockImplementation((input: string) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(json({ success: true, data: { accessToken: "tok", username: "a", role: "EDITOR" }, error: null }));
      }
      if (input === "/api/programs/manage/1") {
        // 두 번째 조회부터는 방금 올린 이미지가 들어 있다 — 데이터가 실제로 달라져야 화면이 다시 채워진다.
        const uploaded = fetchMock.mock.calls.filter(([url]) => url === "/api/programs/manage/1").length > 1;
        const images = uploaded ? [{ id: 42, status: "READY", altText: "현장", url640: "http://x/640.jpg", published: false }] : [];
        return Promise.resolve(json({ success: true, data: { ...ADMIN_DATA, images }, error: null }));
      }
      return Promise.resolve(json({ success: false, data: null, error: { code: "X", message: "x" } }));
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  // 이미지를 올리면 서버 데이터를 다시 불러온다. 그때 저장 전 입력(본문에 넣은 이미지 표시 포함)이 지워지던 버그.
  it("서버 데이터를 다시 불러와도 아직 저장하지 않은 설명을 지우지 않는다", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAuthProvider>
          <ProgramEditPanel programId={1} onPublished={() => {}} />
        </AdminAuthProvider>
      </QueryClientProvider>,
    );

    const textarea = (await screen.findByRole("textbox", { name: "설명" })) as HTMLTextAreaElement;
    await waitFor(() => expect(textarea.value).toBe("저장된 설명"));
    fireEvent.change(textarea, { target: { value: "저장된 설명\n\n![현장](image:42)" } });
    const calls = fetchMock.mock.calls.filter(([url]) => url === "/api/programs/manage/1").length;

    await queryClient.invalidateQueries();

    await waitFor(() =>
      expect(fetchMock.mock.calls.filter(([url]) => url === "/api/programs/manage/1").length).toBeGreaterThan(calls),
    );
    // 다시 불러온 데이터(방금 올린 이미지)가 화면에 반영될 때까지 기다린 뒤에 확인한다.
    await screen.findByRole("img", { name: "현장" });
    expect((screen.getByRole("textbox", { name: "설명" }) as HTMLTextAreaElement).value).toBe("저장된 설명\n\n![현장](image:42)");
  });
});
