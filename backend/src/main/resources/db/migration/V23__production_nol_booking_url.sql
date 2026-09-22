-- 예약 플랫폼이 네이버 예약 하나에서 네이버+놀(NOL by Yanolja) 둘로 늘었다(2026-09-22).
-- 기존 booking_url(네이버)은 그대로 두고 놀 링크를 별도 컬럼으로 더한다(nullable, expand 단계, §13.4).
ALTER TABLE production
    ADD COLUMN nol_booking_url VARCHAR(500) NULL;
