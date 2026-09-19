package com.finediningtheater.site;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사이트 공개 여부. 모든 API 요청의 인가 판정과 index.html 서빙에서 매번 묻기 때문에 짧게
 * 캐시한다 — 관리자가 버튼으로 바꾸면 즉시 무효화하고, DB를 직접 고친 경우에도 5초 안에
 * 반영된다. 행이 없거나 값을 알 수 없으면 비공개로 본다(안전한 쪽).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteVisibilityService {

    static final String SITE_PUBLIC_KEY = "SITE_PUBLIC";

    private final SiteSettingRepository siteSettingRepository;

    private final Cache<String, Boolean> cache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(5)).maximumSize(1).build();

    public boolean isPublic() {
        return Boolean.TRUE.equals(cache.get(SITE_PUBLIC_KEY, key -> load()));
    }

    private boolean load() {
        return siteSettingRepository
                .findById(SITE_PUBLIC_KEY)
                .map(setting -> "true".equals(setting.getValue()))
                .orElse(false);
    }

    @Transactional
    public void setPublic(boolean open) {
        SiteSetting setting =
                siteSettingRepository.findById(SITE_PUBLIC_KEY).orElseGet(() -> new SiteSetting(SITE_PUBLIC_KEY, "false"));
        setting.changeValue(String.valueOf(open));
        siteSettingRepository.save(setting);
        cache.invalidate(SITE_PUBLIC_KEY);
    }
}
