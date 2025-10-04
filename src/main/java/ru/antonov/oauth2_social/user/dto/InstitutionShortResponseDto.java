package ru.antonov.oauth2_social.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.entity.InstitutionType;

import java.util.UUID;

@Builder
@Getter
@Setter
public class InstitutionShortResponseDto {
    private UUID id;
    private InstitutionType type;
    @JsonProperty("short_name")
    private String shortName;
}
