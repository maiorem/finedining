import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import PressClippingManagementPanel from "./PressClippingManagementPanel";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

const CLIPPING = {
  id: 1,
  title: "조선일보 - 파인다이닝 씨어터",
  externalUrl: "https://example.com/article",
  status: "DRAFT",
  images: [],
};

function renderPanel(fetchMock: ReturnType<typeof vi.fn>) {
  vi.stubGlobal("fetch", fetchMock);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <PressClippingManagementPanel />
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("PressClippingManagementPanel", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("등록된 보도자료 목록을 상태와 함께 보여준다", async () => {
    const fetchMock = vi.fn((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accessToken: "t", username: "admin", role: "EDITOR" }, error: null }),
        );
      }
      if (input === "/api/press-clippings/manage") {
        return Promise.resolve(jsonResponse({ success: true, data: [CLIPPING], error: null }));
      }
      return Promise.resolve(jsonResponse({ success: true, data: null, error: null }));
    });

    renderPanel(fetchMock);

    expect(await screen.findByDisplayValue("조선일보 - 파인다이닝 씨어터")).toBeInTheDocument();
    expect(screen.getByDisplayValue("https://example.com/article")).toBeInTheDocument();
    expect(screen.getByText("DRAFT")).toBeInTheDocument();
  });

  it("새 보도자료 추가 폼을 제출하면 생성 요청을 보낸다", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn((input: string, init?: RequestInit) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accessToken: "t", username: "admin", role: "EDITOR" }, error: null }),
        );
      }
      if (input === "/api/press-clippings" && init?.method === "POST") {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { id: 2, title: "새 기사", externalUrl: "https://example.com/new", status: "DRAFT", images: [] },
            error: null,
          }),
        );
      }
      if (input === "/api/press-clippings/manage") {
        return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
      }
      return Promise.resolve(jsonResponse({ success: true, data: null, error: null }));
    });

    renderPanel(fetchMock);

    await user.type(await screen.findByLabelText("제목"), "새 기사");
    await user.type(screen.getByLabelText("기사 링크"), "https://example.com/new");
    await user.click(screen.getByRole("button", { name: "추가하기" }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([input, init]) => input === "/api/press-clippings" && (init as RequestInit | undefined)?.method === "POST",
      );
      expect(createCall).toBeDefined();
      expect(JSON.parse((createCall![1] as RequestInit).body as string)).toEqual({
        title: "새 기사",
        externalUrl: "https://example.com/new",
      });
    });
  });
});
