package ru.antonov.oauth2_social.course.dto;

import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.entity.CourseMaterial;
import ru.antonov.oauth2_social.course.entity.CourseMaterialContent;
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
                .creator(creator == null ? null : ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(creator))
                .users(
                        users.stream()
                                .map(ru.antonov.oauth2_social.user.dto.DtoFactory::makeUserShortResponseDto)
                                .toList()
                )
                .build();
    }

    public static CourseMaterialResponseDto makeCourseMaterialResponseDto(CourseMaterial courseMaterial) {
        return CourseMaterialResponseDto
                .builder()
                .id(courseMaterial.getId())
                .topic(courseMaterial.getTopic())
                .author(ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(courseMaterial.getUser()))
                .content(
                        courseMaterial.getCourseMaterialContent()
                                .stream()
                                .map(DtoFactory::makeCourseMaterialContentResponseDto)
                                .toList()
                )
                .publishedAt(courseMaterial.getPublishedAt())
                .lastChangedAt(courseMaterial.getLastChangedAt())
                .build();
    }

    public static CourseMaterialContentResponseDto makeCourseMaterialContentResponseDto(CourseMaterialContent courseMaterialContent) {
        return CourseMaterialContentResponseDto
                .builder()
                .id(courseMaterialContent.getId())
                .originalFileName(courseMaterialContent.getOriginalFileName())
                .build();
    }


}
