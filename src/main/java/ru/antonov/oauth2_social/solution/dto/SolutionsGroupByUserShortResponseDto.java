package ru.antonov.oauth2_social.solution.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;

import java.util.List;

@Builder
@Getter
@Setter
public class SolutionsGroupByUserShortResponseDto {
    private UserShortResponseDto user;
    private List<SolutionShortResponseDto> solutions;
}
