package com.finediningtheater.artist;

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
class CastingServiceTest {

    @Mock private CastingRepository castingRepository;

    private CastingService service() {
        return new CastingService(castingRepository);
    }

    @Test
    void 한국어_제목이_없으면_발행을_거부한다() {
        Casting casting = new Casting();
        when(castingRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(casting));

        assertThatThrownBy(() -> service().publish(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 발행하면_draft가_공개본으로_승격되고_PUBLISHED가_된다() {
        Casting casting = new Casting();
        CastingTranslation ko = casting.addTranslation(SiteLocale.KO, null, null);
        ko.updateDraft("배우 모집", "지원은 이메일로");
        when(castingRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(casting));

        Casting result = service().publish(1L, 99L);

        assertThat(result.isPublished()).isTrue();
        assertThat(result.translationFor(SiteLocale.KO).getTitle()).isEqualTo("배우 모집");
    }
}
