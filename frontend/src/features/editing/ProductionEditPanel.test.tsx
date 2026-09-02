import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import ProductionEditPanel from "./ProductionEditPanel";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function adminData(status: "DRAFT" | "PUBLISHED") {
  return {
    id: 1,
    slug: "showcase",
    status,
    translations: [
      {
        locale: "KO",
        title: "쇼케이스",
        subtitle: null,
        description: null,
        draftTitle: null,
        draftSubtitle: null,
        draftDescription: null,
        hasPendingDraft: false,
      },
      {
        locale: "EN",
        title: "Showcase",
        subtitle: null,
        description: null,
        draftTitle: null,
        draftSubtitle: null,
        draftDescription: null,
        hasPendingDraft: false,
      },
    ],
    images: [],
  };
}

function renderPanel(status: "DRAFT" | "PUBLISHED") {
  const fetchMock = vi.fn((input: string) => {
    if (input.includes("/api/auth/admin/refresh")) {
      return Promise.resolve(
        jsonResponse({ success: true, data: { accessToken: "t", username: "admin", role: "EDITOR" }, error: null }),
      );
    }
    return Promise.resolve(jsonResponse({ success: true, data: adminData(status), error: null }));
  });
  vi.stubGlobal("fetch", fetchMock);

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <ProductionEditPanel productionId={1} />
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("ProductionEditPanel", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("이미 발행된 작품도 발행 버튼을 계속 보여준다 — 새 임시저장을 밀어 올릴 수 있어야 한다", async () => {
    renderPanel("PUBLISHED");

    expect(await screen.findByRole("button", { name: "발행하기" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "발행취소" })).toBeInTheDocument();
  });

  it("아직 발행되지 않은 작품은 발행취소 버튼을 보여주지 않는다", async () => {
    renderPanel("DRAFT");

    expect(await screen.findByRole("button", { name: "발행하기" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "발행취소" })).not.toBeInTheDocument();
  });

  // 제목·URL을 별개 저장 버튼으로 나눠뒀을 때 운영자가 URL 저장 버튼을 놓쳐 값이 비는 사고가
  // 실제로 있었다 — 저장 버튼 하나로 전부 같이 보내지는지 회귀 테스트로 고정한다.
  it("저장 버튼 하나로 예매·위치 링크까지 함께 저장한다", async () => {
    const user = userEvent.setup();
    const putCalls: string[] = [];
    const fetchMock = vi.fn((input: string, init?: RequestInit) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accessToken: "t", username: "admin", role: "EDITOR" }, error: null }),
        );
      }
      if (init?.method === "PUT") {
        putCalls.push(input);
      }
      return Promise.resolve(jsonResponse({ success: true, data: adminData("DRAFT"), error: null }));
    });
    vi.stubGlobal("fetch", fetchMock);

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAuthProvider>
          <ProductionEditPanel productionId={1} />
        </AdminAuthProvider>
      </QueryClientProvider>,
    );

    await user.type(
      await screen.findByPlaceholderText("https://booking.naver.com/..."),
      "https://booking.naver.com/bizes/1",
    );
    await user.type(screen.getByPlaceholderText("https://map.naver.com/..."), "https://map.naver.com/p/somewhere");
    await user.click(screen.getByRole("button", { name: "임시저장" }));

    await waitFor(() => {
      expect(putCalls).toEqual(
        expect.arrayContaining([
          "/api/productions/1/translations/KO",
          "/api/productions/1/booking-url",
          "/api/productions/1/location-url",
        ]),
      );
    });
  });
});
