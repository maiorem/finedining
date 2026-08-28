package com.finediningtheater.artist;

import com.finediningtheater.global.support.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    // productions.translations까지 넣지 않으면 ProductionRef.from()이 Production의 번역을
    // 읽을 때 세션 밖에서 LazyInitializationException이 난다(2026-08-27 발견, open-in-view: false라
    // 트랜잭션이 끝난 뒤 컨트롤러/DTO 매핑 단계에서 터진다).
    @EntityGraph(attributePaths = {"translations", "productions", "productions.translations"})
    Optional<Artist> findBySlugAndStatus(String slug, ContentStatus status);

    @EntityGraph(attributePaths = "translations")
    List<Artist> findAllByStatusOrderByCreatedAtAsc(ContentStatus status);

    // 관리자용: 상태 무관 조회 + 연결된 작품과 그 작품의 번역까지 한 번에 가져온다.
    @EntityGraph(attributePaths = {"translations", "productions", "productions.translations"})
    Optional<Artist> findWithDetailsById(Long id);

    // 관리자 목록도 ArtistAdminResponse가 productions를 읽으므로 같이 가져온다.
    @EntityGraph(attributePaths = {"translations", "productions", "productions.translations"})
    List<Artist> findAllByOrderByCreatedAtAsc();

    // 관리자 미리보기 전용(§3.9 ?preview=true) — 상태 무관으로 슬러그만으로 찾는다.
    @EntityGraph(attributePaths = {"translations", "productions", "productions.translations"})
    Optional<Artist> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
