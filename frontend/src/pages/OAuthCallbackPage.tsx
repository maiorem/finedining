import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useMemberAuth } from "../contexts/MemberAuthContext";
import styles from "./OAuthCallbackPage.module.css";

/**
 * 카카오 로그인 성공 후 백엔드가 돌려보내는 착지 페이지(CLAUDE.md §7.4). access token은 URL
 * 프래그먼트(#)로 온다 — 서버 로그·Referer에 남지 않는다. 읽자마자 컨텍스트에 저장하고 URL에서
 * 지운 뒤(replace) 홈으로 보낸다. window.location.hash 대신 useLocation()을 쓴다 — BrowserRouter
 * 밖에서(테스트의 MemoryRouter 등) window.location이 갱신되지 않기 때문이다.
 */
export default function OAuthCallbackPage() {
  const { t } = useTranslation();
  const { setSession } = useMemberAuth();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const params = new URLSearchParams(location.hash.replace(/^#/, ""));
    const accessToken = params.get("accessToken");
    const nickname = params.get("nickname");

    if (accessToken && nickname) {
      setSession({ accessToken, nickname });
      navigate("/", { replace: true });
    } else {
      navigate("/login", { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <main className={styles.page}>
      <p className={styles.status}>{t("oauthCallback.processing")}</p>
    </main>
  );
}
