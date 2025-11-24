package ru.antonov.oauth2_social.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.entity.Role;

import java.util.UUID;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserShortResponseDto {
    private UUID id;

    private String name;

    private String surname;

    private String patronymic;

    private String group;

    private Role role;
}
