import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import ProductionEditPanel from "./ProductionEditPanel";

function json(body: unknown): Response {
  return { json: async () => body, ok: true } as Response;
}

const IMAGES = [
  { id: 10, status: "READY", altText: "포스터", url640: "http://x/10.jpg", published: true },
  { id: 20, status: "READY", altText: "현장", url640: "http://x/20.jpg", published: true },
];

function adminData(heroImageId: number | null) {
  return {
    id: 1,
    slug: "new-show",
    status: "PUBLISHED",
    bookingUrl: null,
    nolBookingUrl: null,
    locationUrl: null,
    heroImageId,
    translations: [
      {
        locale: "KO",
        title: "새 공연",
        subtitle: null,
        description: "저장된 설명",
        draftTitle: null,
        draftSubtitle: null,
        draftDescription: null,
        hasPendingDraft: false,
      },
    ],
    images: IMAGES,
  };
}

describe("ProductionEditPanel 대표 이미지", () => {
  const fetchMock = vi.fn();
  // 실제 서버처럼: PUT이 성공하면 그 값을 기억해서 다음 GET(재조회)에 반영한다.
  let heroImageId: number | null = null;

  beforeEach(() => {
    fetchMock.mockReset();
    heroImageId = null;
    vi.stubGlobal("fetch", fetchMock);
    fetchMock.mockImplementation((input: string, init?: RequestInit) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(json({ success: true, data: { accessToken: "tok", username: "a", role: "EDITOR" }, error: null }));
      }
      if (input === "/api/productions/1/hero-image" && init?.method === "PUT") {
        heroImageId = (JSON.parse(String(init.body)) as { heroImageId: number | null }).heroImageId;
        return Promise.resolve(json({ success: true, data: adminData(heroImageId), error: null }));
      }
      if (input === "/api/productions/manage/1") {
        return Promise.resolve(json({ success: true, data: adminData(heroImageId), error: null }));
      }
      return Promise.resolve(json({ success: true, data: adminData(heroImageId), error: null }));
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function renderPanel(variant: "center" | "side" = "center") {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAuthProvider>
          <ProductionEditPanel productionId={1} variant={variant} />
        </AdminAuthProvider>
      </QueryClientProvider>,
    );
  }

  it("center 화면은 올린 사진들을 대표 이미지 후보로 보여주고, 고르면 즉시 저장된다", async () => {
    const user = userEvent.setup();
    renderPanel("center");

    await screen.findByText("대표 이미지로 쓸 사진");
    const useSecond = screen.getAllByRole("button", { name: "이 사진을 대표 이미지로 쓰기" })[1];
    await user.click(useSecond);

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/productions/1/hero-image",
        expect.objectContaining({ method: "PUT", body: JSON.stringify({ heroImageId: 20 }) }),
      ),
    );
    // 서버가 반영한 값을 다시 불러와 두 번째 사진이 "지금 대표 이미지"로 바뀐다.
    expect(await screen.findByRole("button", { name: "지금 대표 이미지로 쓰고 있습니다" })).toBeInTheDocument();
  });

  it("선택 해제를 누르면 대표 이미지 지정을 null로 되돌린다", async () => {
    heroImageId = 10;
    const user = userEvent.setup();
    renderPanel("center");

    await screen.findByRole("button", { name: "선택 해제 (목록 대표사진 사용)" });
    await user.click(screen.getByRole("button", { name: "선택 해제 (목록 대표사진 사용)" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/productions/1/hero-image",
        expect.objectContaining({ method: "PUT", body: JSON.stringify({ heroImageId: null }) }),
      ),
    );
    await waitFor(() => expect(screen.queryByRole("button", { name: "선택 해제 (목록 대표사진 사용)" })).not.toBeInTheDocument());
  });

  it("side 화면(고정 상세용)에는 대표 이미지 선택 UI가 없다", async () => {
    renderPanel("side");

    await screen.findByRole("textbox", { name: "설명" });
    expect(screen.queryByText("대표 이미지로 쓸 사진")).not.toBeInTheDocument();
  });

  it("올린 사진이 없으면 안내 문구를 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input === "/api/auth/admin/refresh") {
        return Promise.resolve(json({ success: true, data: { accessToken: "tok", username: "a", role: "EDITOR" }, error: null }));
      }
      return Promise.resolve(json({ success: true, data: { ...adminData(null), images: [] }, error: null }));
    });

    renderPanel("center");

    expect(await screen.findByText("먼저 위에서 사진을 올려 주세요.")).toBeInTheDocument();
  });
});
