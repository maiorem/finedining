package com.finediningtheater.showing;

import com.finediningtheater.global.support.ContentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowingRepository extends JpaRepository<Showing, Long> {

    // 캘린더 목록·상세 모두 작품 제목을 같이 내려주므로 fetch join으로 N+1을 막는다 (CLAUDE.md §7.3).
    @EntityGraph(attributePaths = {"production", "production.translations"})
    Optional<Showing> findByIdAndStatus(Long id, ContentStatus status);

    @EntityGraph(attributePaths = {"production", "production.translations"})
    List<Showing> findByStatusAndStartsAtBetweenOrderByStartsAtAsc(
            ContentStatus status, Instant from, Instant to);

    @EntityGraph(attributePaths = {"production", "production.translations"})
    List<Showing> findByStatusAndProduction_SlugOrderByStartsAtAsc(ContentStatus status, String slug);
}
