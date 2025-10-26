package ru.antonov.oauth2_social.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.dto.InstitutionShortResponseDto;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CourseResponseDto {
    private UUID id;

    private String name;

    private InstitutionShortResponseDto institution;

    private UserShortResponseDto creator;

    private String code;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    private List<UserShortResponseDto> users;
}
