package ru.antonov.oauth2_social.course.dto;

import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;

public class DtoFactory {
    public static CourseCreateResponseDto makeCourseCreateResponseDto(Course course, User creator, List<User> users){
        return CourseCreateResponseDto
                .builder()
                .id(course.getId())
                .createdAt(course.getCreatedAt())
                .code(course.getCode())
                .name(course.getName())
                .institution(ru.antonov.oauth2_social.user.dto.DtoFactory.makeInstitutionShortResponseDto(course.getInstitution()))
                .creator(ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(creator))
                .users(
                        users.stream()
                                .map(ru.antonov.oauth2_social.user.dto.DtoFactory::makeUserShortResponseDto)
                                .toList()
                )
                .build();
    }


}
