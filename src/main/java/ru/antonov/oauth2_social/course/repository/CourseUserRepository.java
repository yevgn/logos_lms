package ru.antonov.oauth2_social.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.entity.CourseUser;
import ru.antonov.oauth2_social.course.entity.CourseUserKey;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseUserRepository extends JpaRepository<CourseUser, CourseUserKey> {
    @Query(value = "SELECT cu.user FROM CourseUser cu WHERE cu.course.id = :courseId AND cu.isCreator = true")
    Optional<User> findCourseCreatorByCourseId(UUID courseId);

    @Query(value = "SELECT cu.user FROM CourseUser cu WHERE cu.course.id = :courseId")
    List<User> findUsersByCourseId(UUID courseId);

    @Query(value = "SELECT cu FROM CourseUser cu WHERE cu.course.id = :courseId")
    List<CourseUser> findAllByCourseId(UUID courseId);

    @Query(value = """
            SELECT cu FROM CourseUser cu
                JOIN FETCH cu.user
                JOIN FETCH cu.course c
                JOIN FETCH c.institution i
                WHERE i.id = :institutionId
            """)
    List<CourseUser> findAllByInstitutionId(UUID institutionId);

    @Query(value = """
            SELECT cu.course FROM CourseUser cu
             JOIN cu.course c
             JOIN FETCH c.institution i
             WHERE cu.user.id = :userId
            """)
    List<Course> findCoursesByUserId(UUID userId);
}
