package ru.antonov.oauth2_social.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query(value = "SELECT u FROM User u WHERE u.institution.id = :institutionId AND u.group.name = :group")
    List<User> findAllByInstitutionIdAndGroup(UUID institutionId, String group);

    @Query(value = "SELECT u FROM User u WHERE u.institution.id = :institutionId")
    List<User> findAllByInstitutionId(UUID institutionId);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.institution
            LEFT JOIN FETCH u.group
            WHERE u.id IN :idList
            """)
    List<User> findAllByIdList(@Param("idList") List<UUID> idList);

    void deleteByEmail(String email);

    @Query(value = "SELECT u FROM User u WHERE u.group.id IN :groupIdList")
    List<User> findAllByGroupIdList(@Param("groupIdList") List<UUID> groupIdList);

    @Query("SELECT cu.user FROM CourseUser cu WHERE cu.course.id = :courseId")
    List<User> findAllByCourseId(UUID courseId);

    @Query("""
             SELECT u FROM User u
             JOIN FETCH u.institution
             LEFT JOIN FETCH u.group g
             LEFT JOIN FETCH g.institution
             WHERE NOT EXISTS (
                 SELECT cu FROM CourseUser cu
                 WHERE cu.user.id = u.id
                 AND cu.course.id = :courseId
            )
            AND u.institution.id = :institutionId
            """)
    List<User> findAllByInstitutionIdNotInCourse(UUID institutionId, UUID courseId);
}