import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import ProductionEditPanel from "./ProductionEditPanel";

function json(body: unknown): Response {
  return { json: async () => body, ok: true } as Response;
}

function adminData(images: unknown[] = []) {
  return {
    id: 1,
    slug: "new-show",
    status: "DRAFT",
    bookingUrl: null,
    locationUrl: null,
    translations: [
      { locale: "KO", title: "새 공연", subtitle: null, description: "저장된 설명", draftTitle: null, draftSubtitle: null, draftDescription: null, hasPendingDraft: false },
    ],
    images,
  };
}

describe("ProductionEditPanel", () => {
  const fetchMock = vi.fn();
  const calls = () => fetchMock.mock.calls.map(([url, init]) => `${init?.method ?? "GET"} ${url}`);

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
    fetchMock.mockImplementation((input: string) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(json({ success: true, data: { accessToken: "tok", username: "a", role: "EDITOR" }, error: null }));
      }
      if (input === "/api/productions/manage/1") {
        const refetched = fetchMock.mock.calls.filter(([url]) => url === "/api/productions/manage/1").length > 1;
        const images = refetched ? [{ id: 42, status: "READY", altText: "현장", url640: "http://x/640.jpg", published: false }] : [];
        return Promise.resolve(json({ success: true, data: adminData(images), error: null }));
      }
      return Promise.resolve(json({ success: true, data: adminData(), error: null }));
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function renderPanel(props: { variant?: "center" | "side"; onPublished?: () => void } = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAuthProvider>
          <ProductionEditPanel productionId={1} {...props} />
        </AdminAuthProvider>
      </QueryClientProvider>,
    );
    return queryClient;
  }

  it("가운데 화면(center)은 설명을 마크다운 편집기로 쓴다", async () => {
    renderPanel({ variant: "center" });

    expect(await screen.findByRole("button", { name: "이미지 삽입" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "소제목" })).toBeInTheDocument();
  });

  it("옆 패널(side, 고정 상세용)은 예전 입력칸 그대로다", async () => {
    renderPanel({ variant: "side" });

    await screen.findByRole("textbox", { name: "설명" });
    expect(screen.queryByRole("button", { name: "이미지 삽입" })).not.toBeInTheDocument();
  });

  it("서버 데이터를 다시 불러와도 아직 저장하지 않은 설명을 지우지 않는다", async () => {
    const queryClient = renderPanel({ variant: "center" });
    const textarea = (await screen.findByRole("textbox", { name: "설명" })) as HTMLTextAreaElement;
    await waitFor(() => expect(textarea.value).toBe("저장된 설명"));
    fireEvent.change(textarea, { target: { value: "저장된 설명\n\n![현장](image:42)" } });

    await queryClient.invalidateQueries();
    await screen.findByRole("img", { name: "현장" });

    expect((screen.getByRole("textbox", { name: "설명" }) as HTMLTextAreaElement).value).toBe("저장된 설명\n\n![현장](image:42)");
  });

  it("발행하기는 저장한 뒤 발행하고 끝나면 onPublished를 부른다", async () => {
    const user = userEvent.setup();
    const onPublished = vi.fn();
    renderPanel({ variant: "center", onPublished });
    const textarea = (await screen.findByRole("textbox", { name: "설명" })) as HTMLTextAreaElement;
    await waitFor(() => expect(textarea.value).toBe("저장된 설명"));

    await user.click(screen.getByRole("button", { name: "발행하기" }));

    await waitFor(() => expect(onPublished).toHaveBeenCalledTimes(1));
    const order = calls();
    expect(order.indexOf("PUT /api/productions/1/translations/KO")).toBeGreaterThan(-1);
    expect(order.indexOf("POST /api/productions/1/publish")).toBeGreaterThan(order.indexOf("PUT /api/productions/1/translations/KO"));
  });
});
