package ru.antonov.oauth2_social.course.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.course.dto.*;

import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.service.CourseService;

import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;
import ru.antonov.oauth2_social.user.entity.User;


import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CourseController {
    private final CourseService courseService;
    private final AccessManager accessManager;

    @PostMapping("/user-id-list")
    public ResponseEntity<CourseCreateResponseDto> addCourse(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody CourseCreateWithUserIdListRequestDto request
    ){
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        return ResponseEntity.ok(courseService.save(request, principal));
    }

    @PostMapping("/group-id-list")
    public ResponseEntity<CourseCreateResponseDto> addCourse(
            @AuthenticationPrincipal User principal,
            @RequestBody CourseCreateWithGroupIdListRequestDto request
    ){
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        return ResponseEntity.ok(courseService.save(request, principal));
    }

    @PatchMapping("/{courseId}/edit-name")
    public ResponseEntity<?> editCourseName(
            @AuthenticationPrincipal User principal,
            @PathVariable
            UUID courseId,
            @RequestParam("new_name")
            @NotBlank(message = "Поле new_name не может отсутствовать")
            @Size(min = 3, max = 255, message = "Длина поля new_name должна быть больше 2 и не превышать 255 символов")
            String newName
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, true, true);
        courseService.changeCourse(courseId, newName);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCourse(
            @AuthenticationPrincipal User principal,
            @RequestParam("course_id") UUID courseId
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, true, true);
        courseService.deleteById(courseId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{courseId}/add-users/user-id-list")
    public ResponseEntity<List<UserShortResponseDto>> addUsersToCourseByUserIdList(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestBody List<UUID> userIdList
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(courseService.addUsersToCourseByUserIdList(courseId, userIdList, principal));
    }

    @PatchMapping("/{courseId}/add-users/group-id-list")
    public ResponseEntity<List<UserShortResponseDto>> addUsersToCourseByGroupIdList(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestBody List<UUID> groupIdList
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(courseService.addUsersToCourseByGroupIdList(courseId, groupIdList, principal));
    }

    @PatchMapping("/{courseId}/remove-users/user-id-list")
    public ResponseEntity<?> removeUsersFromCourseByUserIdList(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestBody List<UUID> userIdList
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        courseService.removeUsersFromCourseByUserIdList(courseId, userIdList);

        return ResponseEntity.ok().build();
    }

    private void checkPrincipalHasAccessToCourseOrElseThrow(
            User principal, UUID courseId, boolean isNeedToBeCreator, boolean isNeedToHaveHigherRoleThanStudent){
        if(!accessManager.isUserHasAccessToCourse(principal, courseId, isNeedToBeCreator, isNeedToHaveHigherRoleThanStudent)){
            throw new AccessDeniedEx(
                    String.format("Ошибка доступа. У вас нет доступа к курсу %s.", courseId),
                    String.format("Отказ в доступе к курсу. Пользователь %s не имеет доступа к курсу %s",
                            principal.getEmail(), courseId)
            );
        }
    }

    private void checkPrincipalIsAttachedToInstitutionOrElseThrow(User principal) {
        if (principal.getInstitution() == null) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не привязаны к учебному заведению.",
                    String.format("Отказ в доступе при добавлении курса. Пользователь %s не привязан к учебному заведению",
                            principal.getEmail())
            );
        }
    }

}
