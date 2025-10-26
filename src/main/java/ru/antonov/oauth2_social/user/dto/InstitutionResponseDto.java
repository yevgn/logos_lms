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
public class InstitutionResponseDto {
    private UUID id;

    private String email;

    @JsonProperty("short_name")
    private String shortName;

    @JsonProperty("full_name")
    private String fullName;

    private String location;

    @JsonProperty("institution_type")
    private InstitutionType institutionType;

}
