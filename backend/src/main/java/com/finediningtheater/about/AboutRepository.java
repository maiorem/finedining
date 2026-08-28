package com.finediningtheater.about;

import com.finediningtheater.global.support.ContentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AboutRepository extends JpaRepository<AboutContent, Long> {

    // 싱글턴이다 — 항상 가장 먼저 심어진(id가 가장 작은) 행 하나만 존재한다고 가정한다.
    @EntityGraph(attributePaths = "translations")
    Optional<AboutContent> findFirstByOrderByIdAsc();

    // 공개 조회는 항상 PUBLISHED로 필터한다(CLAUDE.md §7.3) — 아직 한 번도 발행 안 됐으면 없는
    // 것과 같다.
    @EntityGraph(attributePaths = "translations")
    Optional<AboutContent> findFirstByStatusOrderByIdAsc(ContentStatus status);
}
