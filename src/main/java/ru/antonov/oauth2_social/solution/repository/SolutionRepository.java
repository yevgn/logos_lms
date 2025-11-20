package ru.antonov.oauth2_social.solution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.solution.entity.Solution;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, UUID> {
    @Query("""
            SELECT s FROM Solution s
            WHERE s.user.id = :userId AND s.task.id = :taskId
            """)
    Optional<Solution> findByUserIdAndTaskId(UUID userId, UUID taskId);
}
