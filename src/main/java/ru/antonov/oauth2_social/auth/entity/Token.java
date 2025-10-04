package ru.antonov.oauth2_social.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tokens")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type")
    private TokenType tokenType;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_mode")
    private TokenMode tokenMode;

    private boolean expired = false;

    private boolean revoked = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    public static Token makeWithDefaults(
            String token,  TokenType tokenType, TokenMode tokenMode, User user){
        return Token
                .builder()
                .token(token)
                .tokenType(tokenType)
                .tokenMode(tokenMode)
                .user(user)
                .build();
    }
}

