package com.finediningtheater.program;

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

/**
 * 공개 조회 + 프로그램(이벤트 공지) 편집 + 이미지 갤러리(§7.5). 네이버 예약과 같은 이유로 캘린더를
 * 자체 구축하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramService {

    private final ProgramRepository programRepository;
    private final MediaService mediaService;

    @Cacheable("programs")
    public List<Program> listPublished() {
        return programRepository.findAllByStatusOrderByCreatedAtDesc(ContentStatus.PUBLISHED);
    }

    @Cacheable(value = "programDetail", key = "#slug")
    public Program getPublished(String slug) {
        return programRepository
                .findBySlugAndStatus(slug, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    /**
     * 관리자 미리보기(§3.9 {@code ?preview=true}) 전용. 상태 무관으로 조회하고 캐시하지 않는다 —
     * DRAFT가 캐시에 올라가면 그다음 익명 요청이 캐시를 맞고 초안을 볼 위험이 생긴다.
     */
    public Program getForPreview(String slug) {
        return programRepository.findBySlug(slug).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public Program getForAdmin(Long id) {
        return programRepository
                .findWithTranslationsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public List<Program> listForAdmin() {
        return programRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Program create(String slug) {
        if (programRepository.existsBySlug(slug)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLUG);
        }
        return programRepository.save(new Program(slug));
    }

    @Transactional
    public void saveDraftTranslation(Long id, SiteLocale locale, String title, String description) {
        Program program = getForAdmin(id);
        ProgramTranslation translation = program.translationRowFor(locale);
        if (translation == null) {
            translation = program.addTranslation(locale, null, null);
        }
        translation.updateDraft(title, description);
    }

    @Transactional
    @CacheEvict(value = {"programs", "programDetail"}, allEntries = true)
    public Program changeApplyUrl(Long id, String applyUrl) {
        Program program = getForAdmin(id);
        program.changeApplyUrl(applyUrl);
        return program;
    }

    @Transactional
    @CacheEvict(value = {"programs", "programDetail"}, allEntries = true)
    public Program changeLocationUrl(Long id, String locationUrl) {
        Program program = getForAdmin(id);
        program.changeLocationUrl(locationUrl);
        return program;
    }

    @Transactional
    @CacheEvict(value = {"programs", "programDetail"}, allEntries = true)
    public Program publish(Long id, Long adminId) {
        Program program = getForAdmin(id);
        program.promoteAllDrafts();
        if (program.titleFor(SiteLocale.KO) == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "한국어 제목을 먼저 입력해 주세요.");
        }
        program.publish(adminId);
        mediaService.publishAllFor(MediaOwnerType.PROGRAM, id);
        return program;
    }

    @Transactional
    @CacheEvict(value = {"programs", "programDetail"}, allEntries = true)
    public Program unpublish(Long id) {
        Program program = getForAdmin(id);
        program.unpublish();
        return program;
    }
}
