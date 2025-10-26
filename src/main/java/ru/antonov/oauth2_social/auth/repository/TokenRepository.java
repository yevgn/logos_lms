package ru.antonov.oauth2_social.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.antonov.oauth2_social.auth.entity.Token;
import ru.antonov.oauth2_social.auth.entity.TokenMode;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {
    Optional<Token> findByToken(String token);

    boolean existsByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE Token SET revoked = true, expired = true" +
            " WHERE user.email = :email AND (tokenMode = 'ACCESS' OR tokenMode = 'REFRESH')")
    int revokeAccessAndRefreshTokensByUserEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE Token SET revoked = true, expired = true" +
            " WHERE user.email = :email AND tokenMode = :tokenMode")
    int revokeAllByEmailAndTokenMode(String email, TokenMode tokenMode);

    @Query("""
           SELECT t.user FROM Token t WHERE t.token = :token
           """)
    Optional<User> findUserByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE Token SET revoked = true, expired = true" +
            " WHERE user.email = :email AND tokenMode IN :modes")
    int revokeAllUsersTokensByTokenModeIn(@Param("email") String email, @Param("modes") List<TokenMode> modes);

    @Query("""
            SELECT t FROM Token t WHERE t.user.email = :user_email
            AND t.revoked = false AND t.expired = false
            """)
    List<Token> findByUserEmailNotRevokedAndNotExpired(@Param("user_email") String userEmail);
}
