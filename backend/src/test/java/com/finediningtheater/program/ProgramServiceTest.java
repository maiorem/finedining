package com.finediningtheater.program;

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
class ProgramServiceTest {

    @Mock private ProgramRepository programRepository;

    private ProgramService service() {
        return new ProgramService(programRepository);
    }

    @Test
    void 한국어_제목이_없으면_발행을_거부한다() {
        Program program = new Program();
        when(programRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(program));

        assertThatThrownBy(() -> service().publish(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 발행하면_draft가_공개본으로_승격되고_PUBLISHED가_된다() {
        Program program = new Program();
        ProgramTranslation ko = program.addTranslation(SiteLocale.KO, null, null);
        ko.updateDraft("여름 시식회", "참가는 구글폼으로");
        when(programRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(program));

        Program result = service().publish(1L, 99L);

        assertThat(result.isPublished()).isTrue();
        assertThat(result.translationFor(SiteLocale.KO).getTitle()).isEqualTo("여름 시식회");
    }

    @Test
    void 참가_링크는_즉시_반영된다() {
        Program program = new Program();
        when(programRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(program));

        Program result = service().changeApplyUrl(1L, "https://forms.gle/abcd");

        assertThat(result.getApplyUrl()).isEqualTo("https://forms.gle/abcd");
    }

    @Test
    void 위치_링크는_즉시_반영된다() {
        Program program = new Program();
        when(programRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(program));

        Program result = service().changeLocationUrl(1L, "https://map.naver.com/p/somewhere");

        assertThat(result.getLocationUrl()).isEqualTo("https://map.naver.com/p/somewhere");
    }
}
