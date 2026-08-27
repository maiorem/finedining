package com.finediningtheater.showing;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShowingTest {

    private Showing showing() {
        return new Showing(
                new Production("showcase"),
                Instant.parse("2026-09-25T10:00:00Z"),
                120,
                "공연장 A",
                "서울특별시 종로구",
                SiteLocale.KO,
                false);
    }

    @Test
    void OPEN이고_예약URL이_있으면_예약_가능하다() {
        Showing showing = showing();
        showing.changeSalesStatus(SalesStatus.OPEN);
        showing.changeBookingUrl("https://booking.naver.com/booking/13/bizes/000");

        assertThat(showing.isBookingAvailable()).isTrue();
    }

    @Test
    void CLOSING_SOON이고_예약URL이_있으면_예약_가능하다() {
        Showing showing = showing();
        showing.changeSalesStatus(SalesStatus.CLOSING_SOON);
        showing.changeBookingUrl("https://booking.naver.com/booking/13/bizes/000");

        assertThat(showing.isBookingAvailable()).isTrue();
    }

    @Test
    void SOLD_OUT이면_예약URL이_있어도_예약_불가능하다() {
        Showing showing = showing();
        showing.changeSalesStatus(SalesStatus.SOLD_OUT);
        showing.changeBookingUrl("https://booking.naver.com/booking/13/bizes/000");

        assertThat(showing.isBookingAvailable()).isFalse();
    }

    @Test
    void 예약URL이_비어있으면_OPEN이어도_예약_불가능하다() {
        Showing showing = showing();
        showing.changeSalesStatus(SalesStatus.OPEN);
        showing.changeBookingUrl(null);

        assertThat(showing.isBookingAvailable()).isFalse();
    }
}
