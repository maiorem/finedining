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
    Optional<Program> findWithTranslationsById(Long id);

    @EntityGraph(attributePaths = "translations")
    List<Program> findAllByOrderByCreatedAtDesc();
}
