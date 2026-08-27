package com.finediningtheater.artist;

import com.finediningtheater.global.support.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CastingRepository extends JpaRepository<Casting, Long> {

    @EntityGraph(attributePaths = "translations")
    List<Casting> findAllByStatusOrderByCreatedAtDesc(ContentStatus status);

    @EntityGraph(attributePaths = "translations")
    Optional<Casting> findWithTranslationsById(Long id);

    @EntityGraph(attributePaths = "translations")
    List<Casting> findAllByOrderByCreatedAtDesc();
}
