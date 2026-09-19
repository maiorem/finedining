package com.finediningtheater.site;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteVisibilityServiceTest {

    @Mock private SiteSettingRepository siteSettingRepository;

    private SiteVisibilityService service() {
        return new SiteVisibilityService(siteSettingRepository);
    }

    @Test
    void 설정_행이_없으면_비공개로_본다() {
        when(siteSettingRepository.findById("SITE_PUBLIC")).thenReturn(Optional.empty());

        assertThat(service().isPublic()).isFalse();
    }

    @Test
    void 값이_true면_공개다() {
        when(siteSettingRepository.findById("SITE_PUBLIC"))
                .thenReturn(Optional.of(new SiteSetting("SITE_PUBLIC", "true")));

        assertThat(service().isPublic()).isTrue();
    }

    @Test
    void 알_수_없는_값은_비공개로_본다() {
        when(siteSettingRepository.findById("SITE_PUBLIC"))
                .thenReturn(Optional.of(new SiteSetting("SITE_PUBLIC", "maybe")));

        assertThat(service().isPublic()).isFalse();
    }

    @Test
    void 공개로_바꾸면_캐시가_즉시_무효화되어_바로_반영된다() {
        SiteSetting setting = new SiteSetting("SITE_PUBLIC", "false");
        when(siteSettingRepository.findById("SITE_PUBLIC")).thenReturn(Optional.of(setting));
        SiteVisibilityService service = service();
        assertThat(service.isPublic()).isFalse();

        service.setPublic(true);

        assertThat(service.isPublic()).isTrue();
    }
}
