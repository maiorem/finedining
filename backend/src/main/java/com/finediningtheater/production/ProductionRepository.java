package com.finediningtheater.production;

import com.finediningtheater.global.support.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionRepository extends JpaRepository<Production, Long> {

    // 목록·상세 응답에서 매번 번역을 조회하므로 fetch join으로 N+1을 막는다 (CLAUDE.md §7.3).
    @EntityGraph(attributePaths = "translations")
    Optional<Production> findBySlugAndStatus(String slug, ContentStatus status);

    @EntityGraph(attributePaths = "translations")
    List<Production> findAllByStatusOrderByCreatedAtAsc(ContentStatus status);
}
