package ru.antonov.oauth2_social.solution.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.course.dto.CourseShortResponseDto;

import java.util.List;

@Builder
@Getter
@Setter
public class SolutionsGroupByCourseShortResponseDto {
    private CourseShortResponseDto course;
    private List<SolutionWithTaskShortResponseDto> solutions;
}
