import { useEffect, type RefObject } from "react";

const SPEED_PX_PER_SECOND = 40;

/**
 * 콘텐츠를 두 번 이어 붙인 가로 스크롤 컨테이너를 오른쪽→왼쪽으로 천천히 흘린다. 반쯤 지나면
 * 처음으로 되돌려 끝없이 이어지는 것처럼 보인다. 사용자가 손가락·트랙패드로 직접 밀어도 같은
 * 규칙으로 앞뒤 끝을 넘겨서 어느 방향으로든 끝이 없다 — 모바일에서 "손가락으로 움직일 수
 * 있게"라는 요청(2026-09-19)은 이 컨테이너가 진짜 스크롤 영역이라서 그대로 성립한다.
 * paused가 true인 동안에는 아무것도 움직이지 않는다(hover·focus·터치·사용자 일시정지).
 */
export function useMarqueeScroll(ref: RefObject<HTMLElement | null>, paused: boolean) {
  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    // 두 번째 사본이 시작하는 지점 = 한 바퀴의 길이. 레이아웃이 바뀔 수 있어 매번 읽는다.
    const loopWidth = () => el.scrollWidth / 2;

    function wrap() {
      const loop = loopWidth();
      if (loop <= 0) return;
      if (el!.scrollLeft >= loop) el!.scrollLeft -= loop;
      else if (el!.scrollLeft <= 0) el!.scrollLeft += loop;
    }

    // 맨 왼쪽(0)에서는 더 뒤로 밀 수 없어 scroll 이벤트가 안 난다 — 1px 안쪽에서 시작해야 뒤쪽으로도 끝없이 밀린다.
    if (el.scrollLeft === 0 && loopWidth() > 0) el.scrollLeft = 1;

    el.addEventListener("scroll", wrap, { passive: true });

    let frame = 0;
    if (!paused) {
      // scrollLeft는 화면 픽셀 단위로 반올림되어 읽히므로 소수 위치는 따로 들고 있다가 쓴다.
      let position = el.scrollLeft;
      let last = performance.now();
      const step = (now: number) => {
        const elapsed = (now - last) / 1000;
        last = now;
        // 사용자가 직접 밀어 위치가 어긋났으면 그 자리에서 이어간다.
        if (Math.abs(el.scrollLeft - position) > 2) position = el.scrollLeft;
        position += SPEED_PX_PER_SECOND * elapsed;
        el.scrollLeft = position;
        frame = requestAnimationFrame(step);
      };
      frame = requestAnimationFrame(step);
    }

    return () => {
      cancelAnimationFrame(frame);
      el.removeEventListener("scroll", wrap);
    };
  }, [ref, paused]);
}
