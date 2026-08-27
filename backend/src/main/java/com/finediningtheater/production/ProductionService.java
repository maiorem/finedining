package com.finediningtheater.production;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 조회만 다룬다. 쓰기(ProductionEditController)는 관리자 로그인 단계에서 추가한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService {

    private final ProductionRepository productionRepository;

    @Cacheable("productions")
    public List<Production> listPublished() {
        return productionRepository.findAllByStatusOrderByCreatedAtAsc(ContentStatus.PUBLISHED);
    }

    @Cacheable(value = "productionDetail", key = "#slug")
    public Production getPublished(String slug) {
        return productionRepository
                .findBySlugAndStatus(slug, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }
}
