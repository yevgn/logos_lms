package ru.antonov.oauth2_social.course.entity;

import ru.antonov.oauth2_social.course.dto.CourseCreateWithGroupIdListRequestDto;
import ru.antonov.oauth2_social.course.dto.CourseCreateWithUserIdListRequestDto;
import ru.antonov.oauth2_social.user.entity.Institution;


public class EntityFactory {
    public static Course makeCourseEntity(
            CourseCreateWithUserIdListRequestDto dto, Institution institution,  String code
    ){
       return  Course.builder()
                .name(dto.getName())
                .institution(institution)
                .code(code)
                .build();
    }

    public static Course makeCourseEntity(
            CourseCreateWithGroupIdListRequestDto dto, Institution institution, String code
    ){
        return  Course.builder()
                .name(dto.getName())
                .institution(institution)
                .code(code)
                .build();
    }
}
