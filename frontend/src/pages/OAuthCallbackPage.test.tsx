import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { MemberAuthProvider, useMemberAuth } from "../contexts/MemberAuthContext";
import OAuthCallbackPage from "./OAuthCallbackPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

function SessionProbe() {
  const { session } = useMemberAuth();
  return <p>{session ? `logged-in:${session.nickname}` : "logged-out"}</p>;
}

function renderAt(path: string) {
  const fetchMock = vi.fn(() =>
    Promise.resolve(jsonResponse({ success: false, data: null, error: { code: "UNAUTHORIZED", message: "x" } })),
  );
  vi.stubGlobal("fetch", fetchMock);

  return render(
    <MemberAuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
          <Route path="/" element={<SessionProbe />} />
          <Route path="/login" element={<p>login-page</p>} />
        </Routes>
      </MemoryRouter>
    </MemberAuthProvider>,
  );
}

describe("OAuthCallbackPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("프래그먼트에 토큰이 있으면 세션에 저장하고 홈으로 보낸다", async () => {
    renderAt(
      "/oauth/callback#accessToken=abc123&nickname=%EA%B9%80%EC%95%84%EB%AC%B4%EA%B0%9C&accountId=7",
    );

    expect(await screen.findByText("logged-in:김아무개")).toBeInTheDocument();
  });

  it("토큰이 없으면 로그인 페이지로 보낸다", async () => {
    renderAt("/oauth/callback");

    expect(await screen.findByText("login-page")).toBeInTheDocument();
  });

  it("accountId가 없으면 로그인 페이지로 보낸다", async () => {
    renderAt("/oauth/callback#accessToken=abc123&nickname=%EA%B9%80%EC%95%84%EB%AC%B4%EA%B0%9C");

    expect(await screen.findByText("login-page")).toBeInTheDocument();
  });
});
