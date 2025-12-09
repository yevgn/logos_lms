package ru.antonov.oauth2_social.solution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.solution.entity.Solution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, UUID> {
    @Query("""
            SELECT s FROM Solution s
            WHERE s.user.id = :userId AND s.task.id = :taskId
            """)
    Optional<Solution> findByUserIdAndTaskId(UUID userId, UUID taskId);

    @Query("""
            SELECT s FROM Solution s
            JOIN FETCH s.task t
            LEFT JOIN FETCH t.creator cr
            LEFT JOIN FETCH cr.institution
            LEFT JOIN FETCH cr.group gr
            LEFT JOIN FETCH gr.institution
            JOIN FETCH t.course c
            JOIN FETCH c.institution
            WHERE c.id = :courseId AND s.user.id = :userId
            """)
    List<Solution> findAllByCourseIdAndUserId(UUID courseId, UUID userId);

    @Query("""
            SELECT s FROM Solution s
            JOIN FETCH s.task t
            LEFT JOIN FETCH t.creator cr
            LEFT JOIN FETCH cr.institution
            LEFT JOIN FETCH cr.group gr
            LEFT JOIN FETCH gr.institution
            JOIN FETCH t.course c
            JOIN FETCH c.institution
            WHERE c.id = :courseId
            """)
    List<Solution> findAllByCourseId(UUID courseId);
}
