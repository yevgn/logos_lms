package ru.antonov.oauth2_social.entity;

import jakarta.persistence.*;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    private String email;

    private String role;

    @Column(name = "is_enabled")
    private boolean isEnabled = false;
}
