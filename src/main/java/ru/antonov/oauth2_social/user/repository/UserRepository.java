package ru.antonov.oauth2_social.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    void deleteByEmail(String email);
}