import { useEffect, useRef, useState } from "react";

/**
 * 헤더의 투명→불투명 전환처럼 스크롤 임계값을 넘었는지만 필요할 때 쓴다.
 * rAF로 스로틀해서 스크롤 핸들러가 매 프레임 레이아웃을 다시 읽지 않게 한다 (CLAUDE.md §8.6 참고).
 */
export function useScrolled(threshold = 8): boolean {
  const [scrolled, setScrolled] = useState(false);
  const ticking = useRef(false);

  useEffect(() => {
    function onScroll() {
      if (ticking.current) return;
      ticking.current = true;
      requestAnimationFrame(() => {
        setScrolled(window.scrollY > threshold);
        ticking.current = false;
      });
    }

    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [threshold]);

  return scrolled;
}
