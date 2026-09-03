import { useAdminAuth } from "../contexts/AdminAuthContext";
import type { AdminRole } from "../api/auth";

export type Capability =
  | "production:edit"
  | "review:moderate"
  | "program:edit"
  | "proposal:review"
  | "artist:edit"
  | "pressClipping:edit";

// 역할이 아니라 능력을 검사한다 (CLAUDE.md §3.3·§9) — 컴포넌트에 `role === 'EDITOR'`를 박지
// 않고 이 매핑 표 하나만 고치면 되게 한다. 지금은 EDITOR/SUPER_ADMIN 모두 콘텐츠 편집 권한이
// 같으므로 로그인 여부와 사실상 같지만, 나중에 역할별로 갈리면 여기만 바뀐다.
const CAPABILITY_ROLES: Record<Capability, readonly AdminRole[]> = {
  "production:edit": ["EDITOR", "SUPER_ADMIN"],
  "review:moderate": ["EDITOR", "SUPER_ADMIN"],
  "program:edit": ["EDITOR", "SUPER_ADMIN"],
  "proposal:review": ["EDITOR", "SUPER_ADMIN"],
  "artist:edit": ["EDITOR", "SUPER_ADMIN"],
  "pressClipping:edit": ["EDITOR", "SUPER_ADMIN"],
};

export function useCan(capability: Capability): boolean {
  const { session } = useAdminAuth();
  if (!session) {
    return false;
  }
  return CAPABILITY_ROLES[capability].includes(session.role);
}
