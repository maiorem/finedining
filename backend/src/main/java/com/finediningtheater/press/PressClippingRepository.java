package com.finediningtheater.press;

import com.finediningtheater.global.support.ContentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PressClippingRepository extends JpaRepository<PressClipping, Long> {

    // 공개 조회는 항상 PUBLISHED로 필터한다 (CLAUDE.md §7.3).
    List<PressClipping> findAllByStatusOrderByCreatedAtDesc(ContentStatus status);

    List<PressClipping> findAllByOrderByCreatedAtDesc();
}
