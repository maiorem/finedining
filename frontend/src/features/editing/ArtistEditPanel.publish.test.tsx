import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import ArtistEditPanel from "./ArtistEditPanel";

function json(body: unknown): Response {
  return { json: async () => body, ok: true } as Response;
}

function translation(locale: "KO" | "EN", name: string | null, bio: string | null) {
  return {
    locale,
    name,
    role: null,
    bio,
    credits: null,
    quote: null,
    draftName: null,
    draftRole: null,
    draftBio: null,
    draftCredits: null,
    draftQuote: null,
    hasPendingDraft: false,
  };
}

function adminData(images: unknown[] = []) {
  return {
    id: 1,
    slug: "kim-miran",
    status: "DRAFT",
    linkUrl: null,
    email: null,
    displayOrder: 0,
    interviewUrl: null,
    translations: [translation("KO", "김미란", "저장된 소개"), translation("EN", null, null)],
    images,
  };
}

describe("ArtistEditPanel 발행 흐름", () => {
  const fetchMock = vi.fn();
  const calls = () => fetchMock.mock.calls.map(([url, init]) => `${init?.method ?? "GET"} ${url}`);

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
    fetchMock.mockImplementation((input: string) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(json({ success: true, data: { accessToken: "tok", username: "a", role: "EDITOR" }, error: null }));
      }
      if (input === "/api/artists/manage/1") {
        // 두 번째 조회부터는 방금 올린 이미지가 들어 있다 — 데이터가 실제로 달라져야 화면이 다시 채워진다.
        const refetched = fetchMock.mock.calls.filter(([url]) => url === "/api/artists/manage/1").length > 1;
        const images = refetched ? [{ id: 42, status: "READY", altText: "현장", url640: "http://x/640.jpg", published: false }] : [];
        return Promise.resolve(json({ success: true, data: adminData(images), error: null }));
      }
      return Promise.resolve(json({ success: true, data: adminData(), error: null }));
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function renderPanel(onPublished = vi.fn()) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAuthProvider>
          <ArtistEditPanel artistId={1} onPublished={onPublished} />
        </AdminAuthProvider>
      </QueryClientProvider>,
    );
    return { queryClient, onPublished };
  }

  it("서버 데이터를 다시 불러와도 아직 저장하지 않은 소개글을 지우지 않는다", async () => {
    const { queryClient } = renderPanel();
    const textarea = (await screen.findByRole("textbox", { name: "소개글" })) as HTMLTextAreaElement;
    await waitFor(() => expect(textarea.value).toBe("저장된 소개"));
    fireEvent.change(textarea, { target: { value: "저장된 소개\n\n![현장](image:42)" } });

    await queryClient.invalidateQueries();
    await screen.findByRole("img", { name: "현장" }); // 다시 불러온 데이터가 화면에 반영된 뒤

    expect((screen.getByRole("textbox", { name: "소개글" }) as HTMLTextAreaElement).value).toBe("저장된 소개\n\n![현장](image:42)");
  });

  it("발행하기는 글·이메일·순서를 저장한 뒤 발행하고 끝나면 onPublished를 부른다", async () => {
    const user = userEvent.setup();
    const { onPublished } = renderPanel();
    await screen.findByRole("textbox", { name: "소개글" });
    await waitFor(() => expect((screen.getByRole("textbox", { name: "소개글" }) as HTMLTextAreaElement).value).toBe("저장된 소개"));

    await user.click(screen.getByRole("button", { name: "발행하기" }));

    await waitFor(() => expect(onPublished).toHaveBeenCalledTimes(1));
    const order = calls();
    const saveKo = order.indexOf("PUT /api/artists/1/translations/KO");
    const people = order.indexOf("PUT /api/artists/1/people-info");
    const publish = order.indexOf("POST /api/artists/1/publish");
    expect(saveKo).toBeGreaterThan(-1);
    expect(people).toBeGreaterThan(saveKo);
    expect(publish).toBeGreaterThan(people);
    expect(order).not.toContain("PUT /api/artists/1/translations/EN"); // 이름이 비어 있는 EN은 보내지 않는다
  });

  it("PIN이 필요하면 발행 대신 PIN 창을 띄우고 onPublished를 부르지 않는다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(json({ success: true, data: { accessToken: "tok", username: "a", role: "EDITOR" }, error: null }));
      }
      if (input === "/api/artists/1/publish" && init?.method === "POST") {
        return Promise.resolve(json({ success: false, data: null, error: { code: "PIN_REQUIRED", message: "PIN" } }));
      }
      return Promise.resolve(json({ success: true, data: adminData(), error: null }));
    });
    const { onPublished } = renderPanel();
    await waitFor(() => expect((screen.getByRole("textbox", { name: "소개글" }) as HTMLTextAreaElement).value).toBe("저장된 소개"));

    await user.click(screen.getByRole("button", { name: "발행하기" }));

    expect(await screen.findByRole("dialog")).toBeInTheDocument();
    expect(onPublished).not.toHaveBeenCalled();
  });
});
