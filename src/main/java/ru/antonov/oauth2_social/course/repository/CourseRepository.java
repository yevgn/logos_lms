package ru.antonov.oauth2_social.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.course.entity.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    @Query("""
            SELECT c FROM Course c
            JOIN FETCH c.institution
            WHERE c.institution.id = :institutionId
            """)
    List<Course> findAllByInstitutionId(UUID institutionId);

    @Query("""
                SELECT c FROM Course c
                JOIN FETCH c.courseUsers cu
                JOIN FETCH cu.user u
                LEFT JOIN FETCH u.group g
                LEFT JOIN FETCH g.institution
                LEFT JOIN FETCH u.institution
                WHERE c.id = :courseId
            """)
    Optional<Course> findByIdWithCourseUsers(UUID courseId);
}
