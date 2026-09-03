import { ApiError } from "./http";

export type MemberSession = {
  accountId: number;
  accessToken: string;
  nickname: string;
};

type ApiEnvelope<T> = {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
};

// 로그인 자체는 여기 없다 — 카카오 OAuth2 리다이렉트(/api/oauth2/authorization/kakao)가
// 로그인이고, 성공하면 백엔드가 곧장 /oauth/callback으로 돌려보낸다(CLAUDE.md §7.4).
async function postMemberAuth(path: "refresh" | "logout"): Promise<MemberSession | null> {
  const response = await fetch(`/api/auth/member/${path}`, { method: "POST" });
  const envelope = (await response.json()) as ApiEnvelope<MemberSession | null>;

  if (!envelope.success) {
    throw new ApiError(envelope.error?.code ?? "UNKNOWN", envelope.error?.message ?? "요청이 실패했습니다.");
  }

  return envelope.data;
}

/** 페이지 로드 시 refresh 쿠키로 세션을 조용히 복구한다. access token은 메모리에만 둔다(§7.4). */
export function refreshMemberSession(): Promise<MemberSession> {
  return postMemberAuth("refresh") as Promise<MemberSession>;
}

export async function logoutMember(): Promise<void> {
  await postMemberAuth("logout");
}
