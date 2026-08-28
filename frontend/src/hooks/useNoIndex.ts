import { useEffect } from "react";

/** 검색엔진에 노출하면 안 되는 페이지에 붙인다 (예: /login §3.5, 리뷰 §3.6·§10). */
export function useNoIndex() {
  useEffect(() => {
    const meta = document.createElement("meta");
    meta.name = "robots";
    meta.content = "noindex, nofollow";
    document.head.appendChild(meta);
    return () => {
      document.head.removeChild(meta);
    };
  }, []);
}
