import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { ReserveButton } from "./ReserveButton";

describe("ReserveButton", () => {
  const sendBeacon = vi.fn();

  beforeEach(() => {
    sendBeacon.mockClear();
    Object.defineProperty(navigator, "sendBeacon", {
      value: sendBeacon,
      configurable: true,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("예약_가능하면_새_창_링크로_렌더되고_클릭_시_트래킹_비콘을_보낸다", async () => {
    const user = userEvent.setup();
    render(
      <ReserveButton
        showingId={1}
        bookingUrl="https://booking.naver.com/booking/13/bizes/000"
        bookingAvailable
        salesStatus="OPEN"
        channel="test"
      />,
    );

    const link = screen.getByRole("link", { name: /네이버 예약으로 이동/ });
    expect(link).toHaveAttribute("href", "https://booking.naver.com/booking/13/bizes/000");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");

    await user.click(link);
    expect(sendBeacon).toHaveBeenCalledTimes(1);
    expect(sendBeacon.mock.calls[0][0]).toBe(
      "/api/showings/1/booking-click",
    );
  });

  it("SOLD_OUT이면_비활성_버튼이고_링크가_아니다", () => {
    render(
      <ReserveButton
        showingId={1}
        bookingUrl="https://booking.naver.com/booking/13/bizes/000"
        bookingAvailable={false}
        salesStatus="SOLD_OUT"
        channel="test"
      />,
    );

    const button = screen.getByRole("button", { name: "매진" });
    expect(button).toBeDisabled();
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });

  it("예약_URL이_없으면_OPEN이어도_비활성_버튼이다", () => {
    render(
      <ReserveButton
        showingId={1}
        bookingUrl={null}
        bookingAvailable={false}
        salesStatus="OPEN"
        channel="test"
      />,
    );

    const button = screen.getByRole("button", { name: "예약 페이지 준비 중" });
    expect(button).toBeDisabled();
  });

  it("ENDED면_비활성_버튼에_종료_문구를_보여준다", () => {
    render(
      <ReserveButton
        showingId={1}
        bookingUrl={null}
        bookingAvailable={false}
        salesStatus="ENDED"
        channel="test"
      />,
    );

    expect(screen.getByRole("button", { name: "종료" })).toBeDisabled();
  });
});
