package ru.antonov.oauth2_social.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.entity.Role;

import java.util.UUID;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserResponseDto {
    private UUID id;

    private String name;

    private String surname;

    private String patronymic;

    private String email;

    private Integer age;

    private InstitutionShortResponseDto institution;

    private Role role;

    private String group;

    @JsonProperty("is_enabled")
    private boolean isEnabled;
}
