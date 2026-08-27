package com.finediningtheater.artist;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.global.support.SiteLocale;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 모집 공고 — 열람 전용, 지원 접수는 범위 밖이다(CLAUDE.md §3.8). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CastingService {

    private final CastingRepository castingRepository;

    @Cacheable("castings")
    public List<Casting> listPublished() {
        return castingRepository.findAllByStatusOrderByCreatedAtDesc(ContentStatus.PUBLISHED);
    }

    public Casting getForAdmin(Long id) {
        return castingRepository.findWithTranslationsById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public List<Casting> listForAdmin() {
        return castingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Casting create() {
        return castingRepository.save(new Casting());
    }

    @Transactional
    public void saveDraftTranslation(Long id, SiteLocale locale, String title, String body) {
        Casting casting = getForAdmin(id);
        CastingTranslation translation = casting.translationRowFor(locale);
        if (translation == null) {
            translation = casting.addTranslation(locale, null, null);
        }
        translation.updateDraft(title, body);
    }

    @Transactional
    @CacheEvict(value = "castings", allEntries = true)
    public Casting publish(Long id, Long adminId) {
        Casting casting = getForAdmin(id);
        casting.promoteAllDrafts();
        if (casting.translationFor(SiteLocale.KO) == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "한국어 제목을 먼저 입력해 주세요.");
        }
        casting.publish(adminId);
        return casting;
    }

    @Transactional
    @CacheEvict(value = "castings", allEntries = true)
    public Casting unpublish(Long id) {
        Casting casting = getForAdmin(id);
        casting.unpublish();
        return casting;
    }
}
