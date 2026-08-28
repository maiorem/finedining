package com.finediningtheater.artist;

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

/** 공개 조회 + 아티스트 편집(기능6) + 프로필 사진 연동(§7.5). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final MediaService mediaService;

    @Cacheable("artists")
    public List<Artist> listPublished() {
        return artistRepository.findAllByStatusOrderByCreatedAtAsc(ContentStatus.PUBLISHED);
    }

    @Cacheable(value = "artistDetail", key = "#slug")
    public Artist getPublished(String slug) {
        return artistRepository
                .findBySlugAndStatus(slug, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public Artist getForAdmin(Long id) {
        return artistRepository.findWithDetailsById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    /**
     * 관리자 미리보기(§3.9 {@code ?preview=true}) 전용. 상태 무관으로 조회하고 캐시하지 않는다 —
     * DRAFT가 캐시에 올라가면 그다음 익명 요청이 캐시를 맞고 초안을 볼 위험이 생긴다.
     */
    public Artist getForPreview(String slug) {
        return artistRepository.findBySlug(slug).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public List<Artist> listForAdmin() {
        return artistRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public Artist create(String slug) {
        if (artistRepository.existsBySlug(slug)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLUG);
        }
        return artistRepository.save(new Artist(slug));
    }

    @Transactional
    public void saveDraftTranslation(
            Long id, SiteLocale locale, String name, String role, String bio, String credits) {
        Artist artist = getForAdmin(id);
        ArtistTranslation translation = artist.translationRowFor(locale);
        if (translation == null) {
            translation = artist.addTranslation(locale, null, null, null, null);
        }
        translation.updateDraft(name, role, bio, credits);
    }

    // draft*처럼 발행을 거치지 않고 즉시 공개본에 반영되는 필드라, 이미 PUBLISHED인 아티스트라면
    // 캐시를 evict하지 않으면 공개 화면에 변경사항이 반영되지 않는다(§7.3 발행 시 캐시 무효화 원칙과
    // 동일 이유).
    @Transactional
    @CacheEvict(value = {"artists", "artistDetail"}, allEntries = true)
    public Artist changeLinkUrl(Long id, String linkUrl) {
        Artist artist = getForAdmin(id);
        artist.changeLinkUrl(linkUrl);
        return artist;
    }

    @Transactional
    @CacheEvict(value = {"artists", "artistDetail"}, allEntries = true)
    public Artist publish(Long id, Long adminId) {
        Artist artist = getForAdmin(id);
        artist.promoteAllDrafts();
        if (artist.nameFor(SiteLocale.KO) == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "한국어 이름을 먼저 입력해 주세요.");
        }
        artist.publish(adminId);
        mediaService.publishAllFor(MediaOwnerType.ARTIST, id);
        return artist;
    }

    @Transactional
    @CacheEvict(value = {"artists", "artistDetail"}, allEntries = true)
    public Artist unpublish(Long id) {
        Artist artist = getForAdmin(id);
        artist.unpublish();
        return artist;
    }
}
