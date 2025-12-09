package ru.antonov.oauth2_social.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.task.entity.TaskComment;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {
    @Query("""
            SELECT tc FROM TaskComment tc
            LEFT JOIN FETCH tc.author a
            LEFT JOIN FETCH a.institution
            LEFT JOIN FETCH a.group g
            LEFT JOIN FETCH g.institution
            JOIN FETCH tc.task t
            LEFT JOIN FETCH t.creator cr
            LEFT JOIN FETCH cr.institution
            LEFT JOIN FETCH cr.group crg
            LEFT JOIN FETCH crg.institution
            JOIN FETCH t.course c
            JOIN FETCH c.institution
            WHERE t.id = :taskId
            ORDER BY tc.publishedAt DESC
            """)
    List<TaskComment> findAllByTaskIdSortByPublishedAt(UUID taskId);

    @Query("""
            SELECT count(tc) FROM TaskComment tc
            WHERE tc.task.id = :taskId
            """)
    int countAllByTaskId(UUID taskId);
}
