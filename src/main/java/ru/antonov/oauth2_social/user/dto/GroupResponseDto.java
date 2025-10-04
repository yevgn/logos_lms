package ru.antonov.oauth2_social.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class GroupResponseDto {
    private UUID id;
    private String name;
    private InstitutionShortResponseDto institution;
}
