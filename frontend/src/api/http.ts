/**
 * 백엔드 ApiResponse<T> 봉투를 벗겨서 데이터만 돌려준다 (CLAUDE.md §7.2).
 * 실패 응답은 코드가 붙은 에러로 던진다 — 컴포넌트에서 error.code로 분기할 수 있다.
 */
export class ApiError extends Error {
  code: string;

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

type ApiEnvelope<T> = {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
};

// 비공개 모드(2026-09-19)에서는 서버가 공개 GET도 관리자 토큰이 있어야 열어준다. 로그인한
// 관리자가 목록 화면에서 401을 맞아 빈 화면을 보지 않도록, 토큰이 있으면 공개 조회에도 실어 보낸다.
type PublicAuthorizer = {
  getToken: () => string | null;
  refreshToken: () => Promise<string | null>;
};

let publicAuthorizer: PublicAuthorizer | null = null;

export function registerPublicAuthorizer(authorizer: PublicAuthorizer) {
  publicAuthorizer = authorizer;
}

async function parseEnvelope<T>(response: Response): Promise<T> {
  const body = (await response.json()) as ApiEnvelope<T>;

  if (!body.success) {
    throw new ApiError(body.error?.code ?? "UNKNOWN", body.error?.message ?? "요청이 실패했습니다.");
  }

  return body.data as T;
}

export async function apiGet<T>(path: string): Promise<T> {
  const token = publicAuthorizer?.getToken() ?? null;
  const response = await fetch(path, token ? { headers: { Authorization: `Bearer ${token}` } } : undefined);
  if (response.status !== 401 || !token || !publicAuthorizer) {
    return parseEnvelope<T>(response);
  }

  // access token이 만료됐을 뿐일 수 있다 — 한 번만 재발급해서 재시도한다.
  const refreshed = await publicAuthorizer.refreshToken();
  if (!refreshed) {
    return parseEnvelope<T>(response);
  }
  return parseEnvelope<T>(await fetch(path, { headers: { Authorization: `Bearer ${refreshed}` } }));
}
