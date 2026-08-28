package com.finediningtheater.about;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.global.support.SiteLocale;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기업·창작집단 소개(CLAUDE.md §1·§6). 회사가 하나뿐이라 싱글턴으로 다룬다 — 생성 API가 없다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AboutService {

    private final AboutRepository aboutRepository;

    @Cacheable("about")
    public AboutContent getPublished() {
        return aboutRepository
                .findFirstByStatusOrderByIdAsc(ContentStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    /** 관리자 미리보기(§3.9 {@code ?preview=true}) 전용. 상태 무관으로 조회하고 캐시하지 않는다. */
    public AboutContent getForPreview() {
        return getForAdmin();
    }

    public AboutContent getForAdmin() {
        return aboutRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Transactional
    public void saveDraftTranslation(SiteLocale locale, String intro) {
        AboutContent about = getForAdmin();
        AboutTranslation translation = about.translationRowFor(locale);
        if (translation == null) {
            translation = about.addTranslation(locale, null);
        }
        translation.updateDraft(intro);
    }

    @Transactional
    @CacheEvict(value = "about", allEntries = true)
    public AboutContent publish(Long adminId) {
        AboutContent about = getForAdmin();
        about.promoteAllDrafts();
        if (about.translationFor(SiteLocale.KO) == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "한국어 소개문을 먼저 입력해 주세요.");
        }
        about.publish(adminId);
        return about;
    }

    @Transactional
    @CacheEvict(value = "about", allEntries = true)
    public AboutContent unpublish() {
        AboutContent about = getForAdmin();
        about.unpublish();
        return about;
    }
}
