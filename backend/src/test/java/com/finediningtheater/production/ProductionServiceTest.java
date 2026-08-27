package com.finediningtheater.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.support.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTest {

    @Mock private ProductionRepository productionRepository;

    private ProductionService productionService() {
        return new ProductionService(productionRepository);
    }

    @Test
    void 목록_조회는_PUBLISHED_상태만_요청한다() {
        when(productionRepository.findAllByStatusOrderByCreatedAtAsc(ContentStatus.PUBLISHED))
                .thenReturn(List.of(new Production("showcase")));

        List<Production> result = productionService().listPublished();

        assertThat(result).hasSize(1);
        verify(productionRepository).findAllByStatusOrderByCreatedAtAsc(ContentStatus.PUBLISHED);
    }

    @Test
    void 존재하지_않거나_DRAFT인_슬러그는_ENTITY_NOT_FOUND를_던진다() {
        when(productionRepository.findBySlugAndStatus("unknown", ContentStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productionService().getPublished("unknown"))
                .isInstanceOf(BusinessException.class);
    }
}
