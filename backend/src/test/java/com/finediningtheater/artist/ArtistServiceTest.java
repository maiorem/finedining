package com.finediningtheater.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import com.finediningtheater.production.ProductionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock private ArtistRepository artistRepository;
    @Mock private ProductionRepository productionRepository;

    private ArtistService service() {
        return new ArtistService(artistRepository, productionRepository);
    }

    @Test
    void 슬러그가_이미_있으면_생성을_거부한다() {
        when(artistRepository.existsBySlug("kim-artist")).thenReturn(true);

        assertThatThrownBy(() -> service().create("kim-artist"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_SLUG));
    }

    @Test
    void 임시저장은_공개본을_바꾸지_않는다() {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");
        when(artistRepository.findWithDetailsById(1L)).thenReturn(Optional.of(artist));

        service().saveDraftTranslation(1L, SiteLocale.KO, "새 이름", "새 역할", "새 소개");

        assertThat(artist.nameFor(SiteLocale.KO)).isEqualTo("김아무개");
        assertThat(artist.translationRowFor(SiteLocale.KO).effectiveName()).isEqualTo("새 이름");
    }

    @Test
    void 한국어_이름이_없으면_발행을_거부한다() {
        Artist artist = new Artist("kim-artist");
        when(artistRepository.findWithDetailsById(1L)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> service().publish(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 발행하면_draft가_공개본으로_승격되고_PUBLISHED가_된다() {
        Artist artist = new Artist("kim-artist");
        ArtistTranslation ko = artist.addTranslation(SiteLocale.KO, null, null, null);
        ko.updateDraft("새 이름", "연출", "소개");
        when(artistRepository.findWithDetailsById(1L)).thenReturn(Optional.of(artist));

        Artist result = service().publish(1L, 99L);

        assertThat(result.isPublished()).isTrue();
        assertThat(result.nameFor(SiteLocale.KO)).isEqualTo("새 이름");
    }

    @Test
    void updateProductions는_기존_연결을_새_목록으로_치환한다() {
        Artist artist = new Artist("kim-artist");
        Production production = new Production("show-1");
        when(artistRepository.findWithDetailsById(1L)).thenReturn(Optional.of(artist));
        when(productionRepository.findAllById(List.of(10L))).thenReturn(List.of(production));

        Artist result = service().updateProductions(1L, List.of(10L));

        assertThat(result.getProductions()).containsExactly(production);
    }
}
