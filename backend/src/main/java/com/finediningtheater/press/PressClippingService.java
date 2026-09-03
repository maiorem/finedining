package com.finediningtheater.press;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 보도자료 — 열람 전용, 등록·수정·발행은 관리자만(§3.5). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PressClippingService {

    private final PressClippingRepository pressClippingRepository;
    private final MediaService mediaService;

    @Cacheable("pressClippings")
    public List<PressClipping> listPublished() {
        return pressClippingRepository.findAllByStatusOrderByCreatedAtDesc(ContentStatus.PUBLISHED);
    }

    public PressClipping getForAdmin(Long id) {
        return findOrThrow(id);
    }

    public List<PressClipping> listForAdmin() {
        return pressClippingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public PressClipping create(String title, String externalUrl) {
        return pressClippingRepository.save(new PressClipping(title, externalUrl));
    }

    @Transactional
    public PressClipping updateContent(Long id, String title, String externalUrl) {
        PressClipping clipping = findOrThrow(id);
        clipping.updateContent(title, externalUrl);
        return clipping;
    }

    /** 발행 시 이미지도 함께 공개로 승격한다 — 같은 트랜잭션에서 호출된다(MediaService 참고). */
    @Transactional
    @CacheEvict(value = "pressClippings", allEntries = true)
    public PressClipping publish(Long id, Long adminId) {
        PressClipping clipping = findOrThrow(id);
        clipping.publish(adminId);
        mediaService.publishAllFor(MediaOwnerType.PRESS_CLIPPING, id);
        return clipping;
    }

    @Transactional
    @CacheEvict(value = "pressClippings", allEntries = true)
    public PressClipping unpublish(Long id) {
        PressClipping clipping = findOrThrow(id);
        clipping.unpublish();
        return clipping;
    }

    private PressClipping findOrThrow(Long id) {
        return pressClippingRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }
}
