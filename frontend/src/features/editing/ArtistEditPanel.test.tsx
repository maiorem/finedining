import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AdminAuthProvider } from "../../contexts/AdminAuthContext";
import ArtistEditPanel from "./ArtistEditPanel";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function adminData(status: "DRAFT" | "PUBLISHED") {
  return {
    id: 1,
    slug: "kim-artist",
    status,
    linkUrl: null,
    translations: [
      {
        locale: "KO",
        name: "김아무개",
        role: null,
        bio: null,
        credits: null,
        draftName: null,
        draftRole: null,
        draftBio: null,
        draftCredits: null,
        hasPendingDraft: false,
      },
      {
        locale: "EN",
        name: "Kim",
        role: null,
        bio: null,
        credits: null,
        draftName: null,
        draftRole: null,
        draftBio: null,
        draftCredits: null,
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
        <ArtistEditPanel artistId={1} />
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("ArtistEditPanel", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("이미 발행된 아티스트도 발행 버튼을 계속 보여준다 — 새 임시저장을 밀어 올릴 수 있어야 한다", async () => {
    renderPanel("PUBLISHED");

    expect(await screen.findByRole("button", { name: "발행하기" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "발행취소" })).toBeInTheDocument();
  });

  it("아직 발행되지 않은 아티스트는 발행취소 버튼을 보여주지 않는다", async () => {
    renderPanel("DRAFT");

    expect(await screen.findByRole("button", { name: "발행하기" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "발행취소" })).not.toBeInTheDocument();
  });

  it("SNS 링크와 참여 작품(자유 텍스트)을 현재 값으로 채워 보여준다", async () => {
    const fetchMock = vi.fn((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({ success: true, data: { accessToken: "t", username: "admin", role: "EDITOR" }, error: null }),
        );
      }
      return Promise.resolve(
        jsonResponse({
          success: true,
          data: {
            ...adminData("PUBLISHED"),
            linkUrl: "https://instagram.com/kimartist",
            translations: [
              {
                locale: "KO",
                name: "김아무개",
                role: null,
                bio: null,
                credits: "쇼케이스 출연 (2024)\n한밤의 만찬 협업 (2023)",
                draftName: null,
                draftRole: null,
                draftBio: null,
                draftCredits: null,
                hasPendingDraft: false,
              },
              adminData("PUBLISHED").translations[1],
            ],
          },
          error: null,
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AdminAuthProvider>
          <ArtistEditPanel artistId={1} />
        </AdminAuthProvider>
      </QueryClientProvider>,
    );

    expect(await screen.findByDisplayValue("https://instagram.com/kimartist")).toBeInTheDocument();
    // getByDisplayValue도 getByText와 같은 기본 정규화(공백 압축)를 적용한다.
    expect(screen.getByDisplayValue("쇼케이스 출연 (2024) 한밤의 만찬 협업 (2023)")).toBeInTheDocument();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
  });
});
