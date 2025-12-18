package ru.antonov.oauth2_social.course.dto;

import ru.antonov.oauth2_social.common.Content;
import ru.antonov.oauth2_social.common.ContentFileResponseDto;
import ru.antonov.oauth2_social.course.entity.*;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;

public class DtoFactory {
    public static CourseResponseDto makeCourseResponseDto(Course course, User creator, List<User> users) {
        return CourseResponseDto
                .builder()
                .id(course.getId())
                .createdAt(course.getCreatedAt())
                .code(course.getCode())
                .name(course.getName())
                .institution(ru.antonov.oauth2_social.user.dto.DtoFactory.makeInstitutionShortResponseDto(course.getInstitution()))
                .members(
                        makeCourseUsersWithCreatorShortResponseDto(creator, users)
                )
                .build();
    }

    public static CourseWithInstitutionAndCreatorShortResponseDto makeCourseWithInstitutionAndCreatorShortResponseDto(
            Course course, User creator
    ) {
        return CourseWithInstitutionAndCreatorShortResponseDto
                .builder()
                .id(course.getId())
                .createdAt(course.getCreatedAt())
                .name(course.getName())
                .institution(ru.antonov.oauth2_social.user.dto.DtoFactory.makeInstitutionShortResponseDto(course.getInstitution()))
                .creator(creator == null ? null : ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(creator))
                .build();
    }

    public static CourseShortResponseDto makeCourseShortResponseDto(Course course) {
        return CourseShortResponseDto
                .builder()
                .id(course.getId())
                .name(course.getName())
                .build();
    }

    public static CourseMaterialResponseDto makeCourseMaterialResponseDto(CourseMaterial courseMaterial) {
        return CourseMaterialResponseDto
                .builder()
                .id(courseMaterial.getId())
                .topic(courseMaterial.getTopic())
                .author(
                        courseMaterial.getUser() != null ?
                                ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(courseMaterial.getUser()) :
                                null
                )
                .content(
                        courseMaterial.getContent() == null ? List.of() :
                        courseMaterial.getContent()
                                .stream()
                                .map(DtoFactory::makeContentFileResponseDto)
                                .toList()
                )
                .publishedAt(courseMaterial.getPublishedAt())
                .lastChangedAt(courseMaterial.getLastChangedAt())
                .build();
    }

    public static ContentFileResponseDto makeContentFileResponseDto(Content content) {
        return ContentFileResponseDto
                .builder()
                .id(content.getId())
                .originalFileName(content.getOriginalFileName())
                .build();
    }

    public static CourseUsersWithCreatorShortResponseDto makeCourseUsersWithCreatorShortResponseDto(
            User creator, List<User> users
    ){
        return CourseUsersWithCreatorShortResponseDto
                .builder()
                .creator(creator == null ? null : ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(creator))
                .users(users == null ?
                        List.of() :
                        users.stream().map(ru.antonov.oauth2_social.user.dto.DtoFactory::makeUserShortResponseDto).toList()
                        )
                .build();
    }

}
