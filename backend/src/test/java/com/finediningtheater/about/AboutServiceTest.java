package com.finediningtheater.about;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.SiteLocale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AboutServiceTest {

    @Mock private AboutRepository aboutRepository;

    private AboutService service() {
        return new AboutService(aboutRepository);
    }

    @Test
    void 한국어_소개문이_없으면_발행을_거부한다() {
        AboutContent about = new AboutContent();
        when(aboutRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(about));

        assertThatThrownBy(() -> service().publish(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 발행하면_draft가_공개본으로_승격되고_PUBLISHED가_된다() {
        AboutContent about = new AboutContent();
        AboutTranslation ko = about.addTranslation(SiteLocale.KO, null);
        ko.updateDraft("파인다이닝 씨어터는...");
        when(aboutRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(about));

        AboutContent result = service().publish(99L);

        assertThat(result.isPublished()).isTrue();
        assertThat(result.translationFor(SiteLocale.KO).getIntro()).isEqualTo("파인다이닝 씨어터는...");
    }

    @Test
    void 임시저장은_공개본을_바꾸지_않는다() {
        AboutContent about = new AboutContent();
        about.addTranslation(SiteLocale.KO, "기존 소개문");
        when(aboutRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(about));

        service().saveDraftTranslation(SiteLocale.KO, "새 소개문");

        assertThat(about.translationFor(SiteLocale.KO).getIntro()).isEqualTo("기존 소개문");
        assertThat(about.translationRowFor(SiteLocale.KO).getDraftIntro()).isEqualTo("새 소개문");
    }
}
