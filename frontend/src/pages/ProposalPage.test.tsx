import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import ProposalPage from "./ProposalPage";

function jsonResponse(body: unknown): Response {
  return { json: async () => body } as Response;
}

async function fillRequiredFields(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("이름"), "김철수");
  await user.type(screen.getByLabelText("회신 이메일"), "chulsoo@example.com");
  await user.type(screen.getByLabelText("제목"), "콜라보 제안");
  await user.type(screen.getByLabelText("내용"), "같이 해보고 싶습니다.");
  await user.click(screen.getByRole("checkbox", { name: /개인정보 수집·이용에 동의합니다/ }));
}

describe("ProposalPage", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("제출에 성공하면 완료 메시지를 보여준다", async () => {
    const user = userEvent.setup();
    fetchMock.mockResolvedValueOnce(jsonResponse({ success: true, data: null, error: null }));

    render(<ProposalPage />);
    await fillRequiredFields(user);
    await user.click(screen.getByRole("button", { name: "제안 보내기" }));

    expect(await screen.findByText("제안을 보냈습니다. 검토 후 회신드리겠습니다.")).toBeInTheDocument();

    const [, options] = fetchMock.mock.calls[0];
    const sentBody = JSON.parse(options.body as string);
    expect(sentBody).toMatchObject({
      name: "김철수",
      contactEmail: "chulsoo@example.com",
      title: "콜라보 제안",
      body: "같이 해보고 싶습니다.",
      privacyConsent: true,
      website: "",
    });
  });

  it("레이트리밋에 걸리면 에러 메시지를 보여준다", async () => {
    const user = userEvent.setup();
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        success: false,
        data: null,
        error: { code: "RATE_LIMITED", message: "x" },
      }),
    );

    render(<ProposalPage />);
    await fillRequiredFields(user);
    await user.click(screen.getByRole("button", { name: "제안 보내기" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
    );
  });

  it("허니팟 필드는 접근성 트리와 탭 순서에서 제외돼 있다", () => {
    render(<ProposalPage />);

    const honeypot = screen.getByLabelText("Website", { selector: "input" });
    expect(honeypot).toHaveAttribute("tabindex", "-1");
    expect(honeypot.closest('[aria-hidden="true"]')).not.toBeNull();
  });
});
