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

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path);
  const body = (await response.json()) as ApiEnvelope<T>;

  if (!body.success) {
    throw new ApiError(body.error?.code ?? "UNKNOWN", body.error?.message ?? "요청이 실패했습니다.");
  }

  return body.data as T;
}
