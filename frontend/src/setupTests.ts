import "@testing-library/jest-dom/vitest";

// jsdom은 matchMedia를 구현하지 않는다. useMediaQuery(편집 패널의 데스크톱 전용 게이트, §8.5)가
// 렌더 중에 이걸 호출하므로 기본값(불일치)을 주는 스텁을 전역에 하나 둔다 — 특정 브레이크포인트를
// 테스트해야 하는 곳에서는 개별 테스트가 이 스텁을 다시 오버라이드한다.
if (!window.matchMedia) {
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }) as MediaQueryList;
}
