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

    // 관리자용: 상태 무관 조회. DRAFT도 봐야 편집할 수 있다 (CLAUDE.md §3.9).
    @EntityGraph(attributePaths = "translations")
    Optional<Production> findWithTranslationsById(Long id);

    @EntityGraph(attributePaths = "translations")
    List<Production> findAllByOrderByCreatedAtAsc();

    // 관리자 미리보기 전용(§3.9 ?preview=true) — 상태 무관으로 슬러그만으로 찾는다.
    @EntityGraph(attributePaths = "translations")
    Optional<Production> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
