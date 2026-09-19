import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { AdminAuthProvider } from "../contexts/AdminAuthContext";
import AboutPage from "./AboutPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

const UNAUTHENTICATED = jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } });

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <AboutPage />
      </AdminAuthProvider>
    </QueryClientProvider>,
  );
}

describe("AboutPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("기본 탭은 정적 소개문을 보여주고 관리 토글은 없다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) return Promise.resolve(UNAUTHENTICATED);
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();
    await screen.findByRole("tab", { name: "소개" });

    // 연혁 슬라이드의 숨김 캡션에도 같은 문장이 있어서 화면에 보이는 문단(p)으로 범위를 좁힌다.
    expect(screen.getByText(/당신의 식탁 위에 이야기를 올립니다\./, { selector: "p" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "소개" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "보도자료 관리" })).not.toBeInTheDocument();
  });

  it("소개 탭에 미션·세 가지 가치·주요 상품군·마지막 문장을 보여준다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) return Promise.resolve(UNAUTHENTICATED);
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();
    await screen.findByRole("tab", { name: "소개" });

    expect(screen.getByText("MISSION")).toBeInTheDocument();
    for (const label of ["STORY", "FOOD", "TOGETHER"]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
    expect(screen.getByText("한 사람의 삶을 듣습니다.")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "주요 상품군" })).toBeInTheDocument();
    for (const label of ["EXPERIENCE CONTENT", "CULTURAL EVENT", "LOCAL CONTENT", "K-FOOD CONTENT", "CUSTOM CONTENT"]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
    expect(screen.getByText(/당신과 다시 마주 앉고 싶습니다\./)).toBeInTheDocument();
    // 연혁 슬라이드 3장 — 그림 속 글은 스크린리더용 캡션으로 함께 실린다.
    expect(screen.getByRole("region", { name: "기업 연혁과 소개" })).toBeInTheDocument();
    expect(screen.getByText(/2025 서울문화재단 서울예술상 후보작 선정/)).toBeInTheDocument();
    expect(screen.getByText(/공연으로 공감하고, 음식으로 경험하고, 대화로 연결됩니다/)).toBeInTheDocument();
    expect(screen.getByText(/프라이빗 씨어터 레스토랑/)).toBeInTheDocument();
    expect(screen.getByText("FINEDINING THEATER")).toBeInTheDocument();
  });

  it("보도자료 탭을 누르면 발행된 보도자료를 외부 링크로 보여준다", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) return Promise.resolve(UNAUTHENTICATED);
      if (input === "/api/press-clippings") {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: [
              {
                id: 1,
                title: "조선일보 - 파인다이닝 씨어터",
                externalUrl: "https://example.com/article",
                imageUrl: "https://example.com/1600.jpg",
                imageAlt: "기사 캡처",
              },
            ],
            error: null,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });

    renderPage();
    await user.click(await screen.findByRole("tab", { name: "보도자료" }));

    const link = await screen.findByRole("link", { name: /조선일보 - 파인다이닝 씨어터/ });
    expect(link).toHaveAttribute("href", "https://example.com/article");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("관리자로 로그인했으면 보도자료 탭에서 관리 토글이 보인다", async () => {
    fetchMock.mockImplementation((input: string) => {
      if (input.includes("/api/auth/admin/refresh")) {
        return Promise.resolve(
          jsonResponse({
            success: true,
            data: { accessToken: "t", username: "admin", role: "EDITOR" },
            error: null,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ success: true, data: [], error: null }));
    });
    const user = userEvent.setup();

    renderPage();
    await user.click(await screen.findByRole("tab", { name: "보도자료" }));

    expect(await screen.findByRole("button", { name: "보도자료 관리" })).toBeInTheDocument();
  });
});
