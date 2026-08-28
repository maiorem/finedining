package com.finediningtheater.media;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    List<MediaAsset> findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(MediaOwnerType ownerType, Long ownerId);

    // 공개 조회는 항상 published=true로 필터한다 (CLAUDE.md §7.3) — 리포지토리 메서드 이름에 박는다.
    List<MediaAsset> findByOwnerTypeAndOwnerIdAndPublishedTrueOrderBySortOrderAsc(
            MediaOwnerType ownerType, Long ownerId);

    int countByOwnerTypeAndOwnerId(MediaOwnerType ownerType, Long ownerId);
}
