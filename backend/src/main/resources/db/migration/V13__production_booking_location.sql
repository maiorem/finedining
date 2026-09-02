-- 회차 캘린더(Showing) 대신 작품에 예매/위치 외부 링크를 직접 붙인다.
-- 네이버 예약이 자체 캘린더를 제공하므로 우리가 캘린더를 자체 구축할 필요가 없다.
ALTER TABLE production
    ADD COLUMN booking_url VARCHAR(500) NULL,
    ADD COLUMN location_url VARCHAR(500) NULL;
