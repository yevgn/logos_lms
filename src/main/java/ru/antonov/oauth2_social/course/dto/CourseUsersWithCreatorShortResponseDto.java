package ru.antonov.oauth2_social.course.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;

import java.util.List;

@Builder
@Getter
@Setter
public class CourseUsersWithCreatorShortResponseDto {
    private UserShortResponseDto creator;
    private List<UserShortResponseDto> users;
}
