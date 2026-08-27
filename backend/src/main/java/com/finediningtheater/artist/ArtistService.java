package com.finediningtheater.artist;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import com.finediningtheater.production.ProductionRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 조회 + 아티스트 편집(기능6). 사진(이미지 파이프라인)은 다음 단계에서 붙인다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ProductionRepository productionRepository;

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
    public void saveDraftTranslation(Long id, SiteLocale locale, String name, String role, String bio) {
        Artist artist = getForAdmin(id);
        ArtistTranslation translation = artist.translationRowFor(locale);
        if (translation == null) {
            translation = artist.addTranslation(locale, null, null, null);
        }
        translation.updateDraft(name, role, bio);
    }

    @Transactional
    public Artist changeLinkUrl(Long id, String linkUrl) {
        Artist artist = getForAdmin(id);
        artist.changeLinkUrl(linkUrl);
        return artist;
    }

    @Transactional
    public Artist updateProductions(Long id, List<Long> productionIds) {
        Artist artist = getForAdmin(id);
        Set<Production> productions = new HashSet<>(productionRepository.findAllById(productionIds));
        artist.replaceProductions(productions);
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
