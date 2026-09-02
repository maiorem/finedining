package com.finediningtheater.program;

import com.finediningtheater.global.support.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    @EntityGraph(attributePaths = "translations")
    List<Program> findAllByStatusOrderByCreatedAtDesc(ContentStatus status);

    @EntityGraph(attributePaths = "translations")
    Optional<Program> findBySlugAndStatus(String slug, ContentStatus status);

    @EntityGraph(attributePaths = "translations")
    Optional<Program> findWithTranslationsById(Long id);

    @EntityGraph(attributePaths = "translations")
    List<Program> findAllByOrderByCreatedAtDesc();

    // 관리자 미리보기 전용(§3.9 ?preview=true) — 상태 무관으로 슬러그만으로 찾는다.
    @EntityGraph(attributePaths = "translations")
    Optional<Program> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
