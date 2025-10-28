package ru.antonov.oauth2_social.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.course.entity.Course;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    @Query(value = "SELECT c FROM Course c WHERE c.institution.id = :institutionId")
    List<Course> findAllByInstitutionId(UUID institutionId);
}
