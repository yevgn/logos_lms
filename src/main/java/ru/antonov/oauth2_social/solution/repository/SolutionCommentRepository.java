package ru.antonov.oauth2_social.solution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.antonov.oauth2_social.solution.entity.SolutionComment;


import java.util.List;
import java.util.UUID;

@Repository
public interface SolutionCommentRepository extends JpaRepository<SolutionComment, UUID> {
    @Query("""
            SELECT sc FROM SolutionComment sc
            LEFT JOIN FETCH sc.author a
            LEFT JOIN FETCH a.institution
            LEFT JOIN FETCH a.group g
            LEFT JOIN FETCH g.institution
            JOIN FETCH sc.solution s
            LEFT JOIN FETCH s.reviewer r
            LEFT JOIN FETCH r.institution
            LEFT JOIN FETCH r.group rg
            LEFT JOIN FETCH rg.institution
            JOIN FETCH s.user su
            LEFT JOIN FETCH su.group sg
            LEFT JOIN FETCH sg.institution
            LEFT JOIN FETCH su.institution
            JOIN FETCH s.task t
            JOIN FETCH t.course c
            JOIN FETCH c.institution
            WHERE s.id = :solutionId
            ORDER BY sc.publishedAt DESC
            """)
    List<SolutionComment> findAllBySolutionIdSortByPublishedAt(UUID solutionId);

    @Query("""
            SELECT count(sc) FROM SolutionComment sc
            WHERE sc.solution.id = :solutionId
            """)
    int countAllBySolutionId(UUID solutionId);
}
