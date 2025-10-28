package ru.antonov.oauth2_social.course.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.course.dto.TaskCreateRequestDto;
import ru.antonov.oauth2_social.course.dto.TaskCreateResponseDto;
import ru.antonov.oauth2_social.course.entity.CourseMaterial;
import ru.antonov.oauth2_social.course.service.TaskService;
import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TaskController {
    private final TaskService taskService;
    private final AccessManager accessManager;

    @PostMapping(value = "/{courseId}", consumes = "multipart/form-data")
    public ResponseEntity<TaskCreateResponseDto> addTaskToCourse(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @Valid @ModelAttribute TaskCreateRequestDto request
            ){

        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(taskService.saveTask(principal, courseId, request));
    }



    private void checkPrincipalHasAccessToCourseOrElseThrow(
            User principal, UUID courseId, boolean isNeedToBeCreator, boolean isNeedToHaveHigherRoleThanStudent) {
        if (!accessManager.isUserHasAccessToCourse(principal, courseId, isNeedToBeCreator, isNeedToHaveHigherRoleThanStudent)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. У вас нет доступа к этому курсу",
                    String.format("Отказ в доступе к курсу. Пользователь %s не имеет доступа к курсу %s",
                            principal.getEmail(), courseId)
            );
        }
    }


}