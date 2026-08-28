package com.finediningtheater.artist;

import com.finediningtheater.global.support.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    @EntityGraph(attributePaths = "translations")
    Optional<Artist> findBySlugAndStatus(String slug, ContentStatus status);

    @EntityGraph(attributePaths = "translations")
    List<Artist> findAllByStatusOrderByCreatedAtAsc(ContentStatus status);

    // 관리자용: 상태 무관 조회.
    @EntityGraph(attributePaths = "translations")
    Optional<Artist> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = "translations")
    List<Artist> findAllByOrderByCreatedAtAsc();

    // 관리자 미리보기 전용(§3.9 ?preview=true) — 상태 무관으로 슬러그만으로 찾는다.
    @EntityGraph(attributePaths = "translations")
    Optional<Artist> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
