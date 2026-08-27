import { ApiError } from "./http";

export type CreateProposalInput = {
  name: string;
  contactEmail: string;
  title: string;
  body: string;
  privacyConsent: boolean;
  website?: string; // 허니팟 — 사람 눈엔 안 보이지만 봇은 채운다 (CLAUDE.md §7.7)
};

type ApiEnvelope = {
  success: boolean;
  data: null;
  error: { code: string; message: string } | null;
};

// 카카오 로그인이 붙기 전까지는 로그인 없이 받는다 (CLAUDE.md §3.7, 2026-08-27 결정).
export async function submitProposal(input: CreateProposalInput): Promise<void> {
  const response = await fetch("/api/proposals", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  const envelope = (await response.json()) as ApiEnvelope;

  if (!envelope.success) {
    throw new ApiError(envelope.error?.code ?? "UNKNOWN", envelope.error?.message ?? "제출에 실패했습니다.");
  }
}
