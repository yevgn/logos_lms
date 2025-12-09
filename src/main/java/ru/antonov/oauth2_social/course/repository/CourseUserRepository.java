package ru.antonov.oauth2_social.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
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

    @Query("""
            SELECT cu FROM CourseUser cu
            JOIN FETCH cu.user u
            LEFT JOIN FETCH u.institution
            LEFT JOIN FETCH u.group g
            LEFT JOIN FETCH g.institution
            JOIN FETCH cu.course c
            JOIN FETCH c.institution
            WHERE cu.course.id = :courseId
            """)
    List<CourseUser> findAllByCourseId(UUID courseId);

    @Query(value = """
            SELECT cu.user u FROM CourseUser cu
            JOIN FETCH cu.user.institution
            LEFT JOIN FETCH cu.user.group g
            LEFT JOIN FETCH g.institution
            WHERE cu.course.id = :courseId AND cu.user.id IN :userIdList
            """)
    List<User> findAllUsersByCourseIdAndUserIdIn(UUID courseId, List<UUID> userIdList);

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

    @Query(value = """
            SELECT cu.course FROM CourseUser cu
            JOIN cu.course c
            JOIN FETCH c.institution i
            WHERE cu.user.id = :userId AND cu.isCreator = :isCreator
            """)
    List<Course> findCoursesByUserIdAndIsCreator(UUID userId, boolean isCreator);

    @Query(value = """
            SELECT count(cu) FROM CourseUser cu WHERE cu.course.id = :courseId
            """)
    int getUserCountForCourseByCourseId(UUID courseId);

    @Query("""
            SELECT cu.user u FROM CourseUser cu
            JOIN FETCH cu.user.institution
            LEFT JOIN FETCH cu.user.group g
            LEFT JOIN FETCH g.institution
            WHERE cu.course.id = :courseId AND cu.isCreator = false
            """)
    List<User> findUsersByCourseIdExcludeCreator(UUID courseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM CourseUser cu WHERE cu.course.id = :courseId AND cu.user.id IN :userIdList")
    void deleteAllByCourseIdAndUserIdIn(UUID courseId, List<UUID> userIdList);
}
