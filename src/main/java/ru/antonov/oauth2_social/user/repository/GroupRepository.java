package ru.antonov.oauth2_social.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.antonov.oauth2_social.user.entity.Group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    @Query(value = "SELECT g FROM Group g WHERE g.institution.id = :institutionId")
    List<Group> findAllByInstitutionId(UUID institutionId);

    @Query(value = "SELECT g FROM Group g WHERE g.institution.id = :institutionId AND g.name = :name")
    Optional<Group> findByInstitutionIdAndName(UUID institutionId, String name);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            DELETE FROM Group g WHERE g.institution.id = :institutionId AND name = :name
            """)
    void deleteByInstitutionIdAndName(UUID institutionId, String name);
}
