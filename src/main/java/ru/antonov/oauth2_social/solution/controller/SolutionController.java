package ru.antonov.oauth2_social.solution.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.solution.dto.*;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.solution.service.SolutionService;
import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.service.UserService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/solutions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SolutionController {
    private final SolutionService solutionService;
    private final UserService userService;
    private final AccessManager accessManager;

    @PostMapping(value = "/task/{taskId}", consumes = "multipart/form-data")
    public ResponseEntity<SolutionResponseDto> addSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            @Valid @ModelAttribute SolutionCreateRequestDto request
    ) {
        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(solutionService.saveSolution(principal, taskId, request));
    }

    @PostMapping(value = "/{solutionId}/revoke")
    public ResponseEntity<?> revokeSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId
    ) {
        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        solutionService.revokeSolution(principal, solutionId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping(value = "/{solutionId}", consumes = "multipart/form-data")
    public ResponseEntity<SolutionResponseDto> updateSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId,
            @Valid @ModelAttribute SolutionUpdateRequestDto request
    ) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        return ResponseEntity.ok(solutionService.updateSolution(principal, solutionId, request));
    }

    @DeleteMapping("/{solutionId}")
    public ResponseEntity<?> deleteSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId
    ) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        solutionService.deleteSolution(principal, solutionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{solutionId}/review")
    public ResponseEntity<?> reviewSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId,
            @Valid @RequestBody SolutionReviewRequestDto request
    ) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        solutionService.reviewSolution(principal, solutionId, request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{solutionId}")
    public ResponseEntity<SolutionResponseDto> findSolutionById(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        return ResponseEntity.ok(solutionService.findSolutionById(principal, solutionId));
    }

    @GetMapping("/task/{taskId}/batch")
    public ResponseEntity<List<SolutionsGroupByUserShortResponseDto>> findSolutionsByTaskIdGroupByUser(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId) {

        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(solutionService.findSolutionsByTaskIdGroupByUser(principal, taskId));
    }

    @GetMapping("/course/{courseId}/user/{userId}/batch")
    public ResponseEntity<List<SolutionsGroupByTaskShortResponseDto>> findSolutionsByCourseIdAndUserIdGroupByTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @PathVariable UUID userId) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Такого пользователя не существует",
                        String.format("Ошибка поиска решений по курсу и id пользователя пользователем %s." +
                                " Пользователя %s не существует", principal.getEmail(), userId)
                ));

        checkPrincipalHasAccessToOtherOrElseThrow(principal, user, true, true);
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        return ResponseEntity.ok(solutionService.findSolutionsByCourseIdAndUserIdGroupByTask(principal, courseId, userId));
    }

    @GetMapping("/task/{taskId}/batch/unreviewed")
    public ResponseEntity<List<SolutionsGroupByUserShortResponseDto>> findUnreviewedSolutionsByTaskIdGroupByUser(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId) {

        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(solutionService.findUnreviewedSolutionsByTaskIdGroupByUser(principal, taskId));
    }

    @GetMapping("/task/{taskId}/user/{userId}")
    public ResponseEntity<SolutionResponseDto> findSolutionByTaskIdAndUserId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            @PathVariable UUID userId) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Такого пользователя не существует",
                        String.format("Ошибка поиска решения по id задания и id пользователя пользователем %s." +
                                " Пользователя %s не существует", principal.getEmail(), userId)
                ));

        checkPrincipalHasAccessToOtherOrElseThrow(principal, user, true, true);
        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(solutionService.findSolutionByTaskIdAndUserId(principal,  taskId, userId));
    }

    @GetMapping("/course/{courseId}/user/{userId}/batch/unreviewed")
    public ResponseEntity<List<SolutionsGroupByTaskShortResponseDto>> findUnreviewedSolutionsByCourseIdAndUserIdGroupByTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @PathVariable UUID userId) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Такого пользователя не существует",
                        String.format("Ошибка поиска решений по курсу и id пользователя пользователем %s." +
                                " Пользователя %s не существует", principal.getEmail(), userId)
                ));

        checkPrincipalHasAccessToOtherOrElseThrow(principal, user, true, true);
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        return ResponseEntity.ok(
                solutionService.findUnreviewedSolutionsByCourseIdAndUserIdGroupByTask(principal, courseId, userId)
        );
    }

    // todo сделать загрузку решений по общим и индивидуальным заданиям

    @GetMapping("/course/{courseId}/batch")
    public ResponseEntity<List<SolutionsGroupByTaskShortResponseDto>> findSolutionsByCourseIdGroupByTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId) {

        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(
                solutionService.findSolutionsByCourseIdGroupByTask(principal, courseId)
        );
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

    private void checkPrincipalIsStudentOrElseThrow(User principal) {
        if (principal.getRole() != Role.STUDENT) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Выполнять это действие могут только пользователи с ролью Студент",
                    String.format("Отказ в доступе на выполнение действия. У пользователя %s с ролью %s" +
                                    "нет прав на выполнение действия",
                            principal.getEmail(), principal.getRole().name())
            );
        }
    }

    private void checkPrincipalIsTutorOrElseThrow(User principal) {
        if (principal.getRole() != Role.TUTOR) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Выполнять это действие могут только пользователи с ролью Преподаватель",
                    String.format("Отказ в доступе на выполнение действия. У пользователя %s с ролью %s" +
                                    "нет прав на выполнение действия",
                            principal.getEmail(), principal.getRole().name())
            );
        }
    }

    private void checkPrincipalHasAccessToTaskOrElseThrow(User principal, UUID taskId) {
        if (!accessManager.isPrincipalHasAccessToTask(principal, taskId)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. У вас нет доступа к этому заданию",
                    String.format("Отказ в доступе к заданию. Пользователь %s не имеет доступа к заданию %s",
                            principal.getEmail(), taskId)
            );
        }
    }

    private void checkPrincipalHasAccessToSolutionOrElseThrow(User principal, UUID solutionId) {
        if (!accessManager.isPrincipalHasAccessToSolution(principal, solutionId)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. У вас нет доступа к этому решению",
                    String.format("Отказ в доступе к решению. Пользователь %s не имеет доступа к решению %s",
                            principal.getEmail(), solutionId)
            );
        }
    }

    private void checkPrincipalIsTaskAuthorOrCourseCreatorOrElseThrow(
            User principal, Task task
    ) {
        if (!principal.equals(task.getCreator()) &&
                !accessManager.isUserHasAccessToCourse(
                        principal, task.getCourse().getId(), true, true)
        ) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не являетесь создателем курса или автором задания",
                    String.format(
                            "Ошибка доступа. Пользователь %s не является создателем курса %s или" +
                                    " автором задания %s",
                            principal.getId(), task.getCourse().getId(), task.getId()
                    )
            );
        }
    }

    private void checkPrincipalHasAccessToOtherOrElseThrow(User principal, User other, boolean isNeedToHaveHigherPriority,
                                                           boolean isNeedToBeInOneInstitute) {
        if (Objects.equals(principal.getId(), other.getId())) {
            return;
        }

        if (!accessManager.isUserHasAccessToOther(principal, other, isNeedToHaveHigherPriority, isNeedToBeInOneInstitute)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не имеете доступа к этому пользователю",
                    String.format("Пользователю %s не разрешен доступ к пользователю %s", principal.getEmail(), other.getEmail())
            );
        }
    }
}
