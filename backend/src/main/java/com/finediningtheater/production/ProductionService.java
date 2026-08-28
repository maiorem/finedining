package com.finediningtheater.production;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 조회 + 작품 편집(2순위: 작품 아카이빙) + 이미지 파이프라인 연동(§7.5). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService {

    private final ProductionRepository productionRepository;
    private final MediaService mediaService;

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

    /** 관리자용. 상태 무관 — DRAFT도 봐야 편집할 수 있다. */
    public Production getForAdmin(Long id) {
        return productionRepository
                .findWithTranslationsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    /**
     * 관리자 미리보기(§3.9 {@code ?preview=true}) 전용. 상태 무관으로 조회하고 캐시하지 않는다 —
     * DRAFT가 캐시에 올라가면 그다음 익명 요청이 캐시를 맞고 초안을 볼 위험이 생긴다.
     */
    public Production getForPreview(String slug) {
        return productionRepository.findBySlug(slug).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public List<Production> listForAdmin() {
        return productionRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public Production create(String slug) {
        if (productionRepository.existsBySlug(slug)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLUG);
        }
        return productionRepository.save(new Production(slug));
    }

    /** "임시저장" — 공개본(title/subtitle/description)은 건드리지 않고 draft에만 쓴다 (CLAUDE.md §3.9). */
    @Transactional
    public void saveDraftTranslation(Long id, SiteLocale locale, String title, String subtitle, String description) {
        Production production = getForAdmin(id);
        ProductionTranslation translation = production.translationRowFor(locale);
        if (translation == null) {
            translation = production.addTranslation(locale, null, null);
        }
        translation.updateDraft(title, subtitle, description);
    }

    /** "발행" — draft를 공개본으로 승격하고 PUBLISHED로 바꾼다. 한국어 제목 없이는 발행할 수 없다. */
    @Transactional
    @CacheEvict(value = {"productions", "productionDetail"}, allEntries = true)
    public Production publish(Long id, Long adminId) {
        Production production = getForAdmin(id);
        production.promoteAllDrafts();
        if (production.titleFor(SiteLocale.KO) == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "한국어 제목을 먼저 입력해 주세요.");
        }
        production.publish(adminId);
        mediaService.publishAllFor(MediaOwnerType.PRODUCTION, id);
        return production;
    }

    @Transactional
    @CacheEvict(value = {"productions", "productionDetail"}, allEntries = true)
    public Production unpublish(Long id) {
        Production production = getForAdmin(id);
        production.unpublish();
        return production;
    }
}
