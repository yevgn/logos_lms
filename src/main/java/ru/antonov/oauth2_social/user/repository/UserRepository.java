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

    @Query(value = "SELECT u FROM User u WHERE u.institution.id = :institutionId AND u.group = :group")
    List<User> findAllByInstitutionIdAndGroup(UUID institutionId, String group);

    @Query(value = "SELECT u FROM User u WHERE u.institution.id = :institutionId")
    List<User> findAllByInstitutionId(UUID institutionId);

    @Query(value = "SELECT u FROM User u WHERE u.id IN :idList")
    List<User> findAllByIdList(@Param("idList") List<UUID> idList);

    void deleteByEmail(String email);

    @Query(value = "SELECT u FROM User u WHERE u.group.id IN :groupIdList")
    List<User> findAllByGroupIdList(@Param("groupIdList") List<UUID> groupIdList);

    @Query("SELECT cu.user FROM CourseUser cu WHERE cu.course.id = :courseId")
    List<User> findAllByCourseId(UUID courseId);

    @Query("SELECT cu.user FROM CourseUser cu WHERE cu.user.institution.id = :institutionId AND " +
            "cu.course.id != :courseId")
    List<User> findAllByInstitutionIdNotInCourse(UUID institutionId, UUID courseId);
}