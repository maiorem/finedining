package com.finediningtheater.media;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    List<MediaAsset> findByProductionIdOrderBySortOrderAsc(Long productionId);

    // 공개 조회는 항상 published=true로 필터한다 (CLAUDE.md §7.3) — 리포지토리 메서드 이름에 박는다.
    List<MediaAsset> findByProductionIdAndPublishedTrueOrderBySortOrderAsc(Long productionId);

    int countByProductionId(Long productionId);
}
