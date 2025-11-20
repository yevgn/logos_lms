package ru.antonov.oauth2_social.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.user.entity.Institution;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
    @Query("""
            SELECT i FROM User u JOIN u.institution i WHERE u.id = :userId
            """)
    Optional<Institution> findByUserId(UUID userId);
}
