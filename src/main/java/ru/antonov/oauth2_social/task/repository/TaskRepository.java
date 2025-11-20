package ru.antonov.oauth2_social.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.task.entity.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.taskUsers tu
        JOIN FETCH tu.user u
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.group
        LEFT JOIN FETCH t.creator cr
        LEFT JOIN FETCH cr.institution
        LEFT JOIN FETCH cr.group
        LEFT JOIN FETCH t.course c
        LEFT JOIN FETCH c.institution
        WHERE t.id = :taskId
    """)
    Optional<Task> findByIdWithTaskUsers( UUID taskId);

    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.solutions s
        LEFT JOIN FETCH t.creator cr
        LEFT JOIN FETCH cr.institution
        LEFT JOIN FETCH cr.group
        LEFT JOIN FETCH t.course c
        LEFT JOIN FETCH c.institution
        LEFT JOIN FETCH s.user u
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.group
        LEFT JOIN FETCH s.reviewer r
        LEFT JOIN FETCH r.institution
        LEFT JOIN FETCH r.group
    
        WHERE t.id = :taskId
    """)
    Optional<Task> findByIdWithSolutions( UUID taskId);

    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.creator cr
        LEFT JOIN FETCH cr.institution
        LEFT JOIN FETCH cr.group
        LEFT JOIN FETCH t.course c
        LEFT JOIN FETCH c.institution
        WHERE t.course.id = :courseId AND t.isForEveryone = TRUE
    """)
    List<Task> findAllCommonTasksByCourseId(UUID courseId);

    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.creator cr
        LEFT JOIN FETCH cr.institution
        LEFT JOIN FETCH cr.group
        LEFT JOIN FETCH t.course c
        LEFT JOIN FETCH c.institution
        WHERE t.course.id = :courseId
    """)
    List<Task> findAllByCourseId(UUID courseId);

    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.taskUsers tu
        LEFT JOIN FETCH tu.user u
        LEFT JOIN FETCH t.creator cr
        LEFT JOIN FETCH cr.institution
        LEFT JOIN FETCH cr.group
        LEFT JOIN FETCH t.course c
        LEFT JOIN FETCH c.institution
        WHERE c.id = :courseId AND tu.user.id = :userId
    """)
    List<Task> findAllTargetTasksWithTaskUsersByCourseIdAndUserId(UUID courseId, UUID userId);
}
