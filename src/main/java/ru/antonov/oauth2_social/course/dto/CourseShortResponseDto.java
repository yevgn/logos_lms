package ru.antonov.oauth2_social.course.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class CourseShortResponseDto {
    private UUID id;
    private String name;
}
