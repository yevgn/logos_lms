package ru.antonov.oauth2_social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.entity.CourseUser;
import ru.antonov.oauth2_social.course.entity.CourseUserKey;
import ru.antonov.oauth2_social.course.repository.CourseRepository;
import ru.antonov.oauth2_social.course.repository.CourseUserRepository;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessManager {
    private final CourseUserRepository courseUserRepository;
    private final CourseRepository courseRepository;

    public boolean isUserHasAccessToOther(User userRequestedForAccess, User toAccessTo, boolean isNeedToHaveHigherPriority,
                                          boolean isNeedToBeInOneInstitution){

        boolean isHasAccess;

        if(isNeedToHaveHigherPriority){
            isHasAccess = userRequestedForAccess.getRole().getPriorityValue() > toAccessTo.getRole().getPriorityValue();
        } else{
            isHasAccess = userRequestedForAccess.getRole().getPriorityValue() >= toAccessTo.getRole().getPriorityValue();
        }

        if(isNeedToBeInOneInstitution){
            Objects.requireNonNull(
                    userRequestedForAccess.getInstitution(),
                    "Ошибка при проверке isUserHasAccessToOther() в AccessManager. institution у userRequestedForAccess must not be null"
            );
            isHasAccess = isHasAccess && Objects.equals(userRequestedForAccess.getInstitution().getId(), toAccessTo.getInstitution().getId());
        }

        return isHasAccess;
    }

    public boolean isUserHasAccessToInstitution(User userRequestedForAccess, UUID instituteId){
        Objects.requireNonNull(
                userRequestedForAccess.getInstitution(),
                "Ошибка при проверке isUserHasAccessToInstitution() в AccessManager. institution у userRequestedForAccess must not be null"
        );

        return Objects.equals(userRequestedForAccess.getInstitution().getId(), instituteId);
    }

    public boolean isUserHasAccessToCourse(
            User user, UUID courseId,  boolean isNeedToBeCreator, boolean isNeedToHaveHigherRoleThanStudent){
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if(courseOpt.isEmpty()){
            return false;
        }

        Course course = courseOpt.get();

        if(user.getRole().equals(Role.ADMIN)){
            Objects.requireNonNull(
                    user.getInstitution(),
                    "Ошибка при проверке isUserHasAccessToCourse() в AccessManager. institution у user must not be null"
            );
            return Objects.equals(user.getInstitution(), course.getInstitution());
        }

        Optional<CourseUser> courseUserOpt = courseUserRepository.findById(
                CourseUserKey
                        .builder()
                        .userId(user.getId())
                        .courseId(courseId)
                        .build()
        );

        // если юзер - админ, то только проверяем равенство его института институту курса
        // если не админ, то проверяем наличие courseUser

        if(courseUserOpt.isEmpty()){
            return false;
        }

        CourseUser courseUser = courseUserOpt.get();

        if(isNeedToBeCreator){
            return courseUser.isCreator();
        }

        if(isNeedToHaveHigherRoleThanStudent){
            return user.getRole().getPriorityValue() > Role.STUDENT.getPriorityValue();
        }

        return true;
    }


//    public boolean isAttachedToInstitution(User user){
//        return user.getInstitution() != null;
//    }
}
