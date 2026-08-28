import { render, screen } from "@testing-library/react";
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
});
