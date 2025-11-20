package ru.antonov.oauth2_social.solution.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.task.dto.TaskShortResponseDto;

import java.util.List;

@Builder
@Getter
@Setter
public class SolutionsGroupByTaskShortResponseDto {
    private TaskShortResponseDto task;
    private List<SolutionWithUserShortResponseDto> solutions;
}
