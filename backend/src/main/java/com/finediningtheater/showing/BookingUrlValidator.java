package com.finediningtheater.showing;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 예약 URL은 저장 시 호스트 화이트리스트로 검증한다 — 오타 하나로 예약이 통째로 죽는다
 * (CLAUDE.md §4). 생성자 주입만 쓴다는 원칙(§11)을 지키려고 {@code @Value}를 생성자
 * 파라미터로 받는 별도 컴포넌트로 뺐다 — ShowingService에 필드 주입을 두지 않는다.
 */
@Component
public class BookingUrlValidator {

    private final Set<String> allowedHosts;

    public BookingUrlValidator(@Value("${app.booking.allowed-hosts}") String allowedHostsRaw) {
        this.allowedHosts =
                Arrays.stream(allowedHostsRaw.split(","))
                        .map(String::trim)
                        .filter(host -> !host.isBlank())
                        .map(host -> host.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
    }

    public void validate(String bookingUrl) {
        String host;
        try {
            host = new URI(bookingUrl).getHost();
        } catch (URISyntaxException | NullPointerException e) {
            host = null;
        }

        if (host == null || !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "허용되지 않은 예약 플랫폼입니다. 네이버 예약 URL만 등록할 수 있습니다.");
        }
    }
}
