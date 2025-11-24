package ru.antonov.oauth2_social.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.dto.InstitutionShortResponseDto;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;

import java.time.LocalDateTime;

import java.util.UUID;

@Builder
@Getter
@Setter
public class CourseShortResponseDto {
    private UUID id;

    private String name;

    private InstitutionShortResponseDto institution;

    private UserShortResponseDto creator;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
