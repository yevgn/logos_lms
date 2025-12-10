package ru.antonov.oauth2_social.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.course.entity.CourseMaterial;

import java.util.UUID;

@Repository
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, UUID> {
    @Query("""
            SELECT count(cm) FROM CourseMaterial cm
            WHERE cm.course.id = :courseId 
            """)
    long countAllByCourseId(UUID courseId);
}
