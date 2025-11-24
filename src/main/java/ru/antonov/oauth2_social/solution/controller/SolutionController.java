package ru.antonov.oauth2_social.solution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.antonov.oauth2_social.common.Content;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.exception.FileNotFoundEx;
import ru.antonov.oauth2_social.exception.IOEx;
import ru.antonov.oauth2_social.solution.common.SortBy;
import ru.antonov.oauth2_social.solution.dto.*;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.solution.service.SolutionService;
import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;

import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.service.UserService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/solutions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SolutionController {
    private final SolutionService solutionService;
    private final UserService userService;
    private final AccessManager accessManager;

    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    @Operation(
            summary = "Добавление решения к заданию с указанным id",
            description = "Требуется роль STUDENT",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому заданию"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Поле content не может быть пустым или отсутствовать"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Конфликт данных",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Вы уже загрузили решение для этого задания"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при записи файлов на диск",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping(value = "/task/{taskId}", consumes = "multipart/form-data")
    public ResponseEntity<SolutionResponseDto> addSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            @Valid @ModelAttribute SolutionCreateRequestDto request
    ) {
        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(solutionService.saveSolution(principal, taskId, request));
    }

    @Operation(
            summary = "Отзыв решения по id",
            description = "Требуется роль STUDENT",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого решения не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Конфликт версий данных",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Решение было обновлено. Повторите попытку"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping(value = "/{solutionId}/revoke")
    public ResponseEntity<?> revokeSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId
    ) {
        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        solutionService.revokeSolution(principal, solutionId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Обновление решения по id",
            description = "Требуется роль STUDENT",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого решения не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Конфликт версий данных",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Решение было обновлено. Повторите попытку"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при записи/удалении данных на диск/с диска",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PatchMapping(value = "/{solutionId}", consumes = "multipart/form-data")
    public ResponseEntity<SolutionResponseDto> updateSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId,
            @Valid @ModelAttribute SolutionUpdateRequestDto request
    ) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        return ResponseEntity.ok(solutionService.updateSolution(principal, solutionId, request));
    }

    @Operation(
            summary = "Удаление решения по id",
            description = "Требуется роль STUDENT",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого решения не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Конфликт версий данных",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Решение было обновлено. Повторите попытку"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при удалении файлов с диска",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @DeleteMapping("/{solutionId}")
    public ResponseEntity<?> deleteSolution(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId
    ) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        solutionService.deleteSolution(principal, solutionId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Проверка решения по id",
            description = "Требуется роль TUTOR или ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого решения не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Конфликт версий данных",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Решение было обновлено. Повторите попытку"
                                        }
                                    """)
                    ))
    }
    )
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


    @Operation(
            summary = "Получение информации о решении по id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого решения не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{solutionId}")
    public ResponseEntity<SolutionResponseDto> findSolutionById(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        return ResponseEntity.ok(solutionService.findSolutionById(principal, solutionId));
    }

    @Operation(
            summary = "Получение решений всех студентов к заданию с указанным id",
            description = "Требуется роль TUTOR или ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому заданию"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого задания не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/task/{taskId}/batch")
    public ResponseEntity<List<SolutionWithUserShortResponseDto>> findSolutionsByTaskId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"SOLUTION_SUBMITTED_AT", "USER"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy) {

        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(solutionService.findSolutionsByTaskId(principal, taskId, sortBy));
    }

    @Operation(
            summary = "Получение НЕПРОВЕРЕННЫХ решений всех студентов к заданию с указанным id",
            description = "Требуется роль TUTOR или ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому заданию"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого задания не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/task/{taskId}/batch/unreviewed")
    public ResponseEntity<List<SolutionWithUserShortResponseDto>> findUnreviewedSolutionsByTaskId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"SOLUTION_SUBMITTED_AT", "USER"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy) {

        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(solutionService.findSolutionsByTaskId(principal, taskId, sortBy, true));
    }

    @Operation(
            summary = "Получение ПРОВЕРЕННЫХ решений всех студентов к заданию с указанным id",
            description = "Требуется роль TUTOR или ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому заданию"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого задания не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/task/{taskId}/batch/reviewed")
    public ResponseEntity<List<SolutionWithUserShortResponseDto>> findReviewedSolutionsByTaskId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"SOLUTION_SUBMITTED_AT", "USER"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy) {

        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(solutionService.findSolutionsByTaskId(principal, taskId, sortBy, false));
    }

    @Operation(
            summary = "Получение решений пользователя с id userId для всех заданий в курсе с id courseId",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого курса не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/course/{courseId}/user/{userId}/batch")
    public ResponseEntity<List<SolutionWithTaskShortResponseDto>> findSolutionsByCourseIdAndUserId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @PathVariable UUID userId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"SOLUTION_SUBMITTED_AT", "TASK_PUBLISHED_AT"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy
    ) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Такого пользователя не существует",
                        String.format("Ошибка поиска решений по курсу и id пользователя пользователем %s." +
                                " Пользователя %s не существует", principal.getEmail(), userId)
                ));

        checkPrincipalHasAccessToOtherOrElseThrow(principal, user, true, true);
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        return ResponseEntity.ok(solutionService.findSolutionsByCourseIdAndUserId(principal, courseId, userId, sortBy));
    }

    @Operation(
            summary = "Получение НЕПРОВЕРЕННЫХ решений пользователя с id userId для всех заданий в курсе с id courseId",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого курса не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/course/{courseId}/user/{userId}/batch/unreviewed")
    public ResponseEntity<List<SolutionWithTaskShortResponseDto>> findUnreviewedSolutionsByCourseIdAndUserId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @PathVariable UUID userId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"SOLUTION_SUBMITTED_AT", "TASK_PUBLISHED_AT"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Такого пользователя не существует",
                        String.format("Ошибка поиска решений по курсу и id пользователя пользователем %s." +
                                " Пользователя %s не существует", principal.getEmail(), userId)
                ));

        checkPrincipalHasAccessToOtherOrElseThrow(principal, user, true, true);
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        return ResponseEntity.ok(
                solutionService.findSolutionsByCourseIdAndUserId(principal, courseId, userId, sortBy, true)
        );

    }

    @Operation(
            summary = "Получение ПРОВЕРЕННЫХ решений пользователя с id userId для всех заданий в курсе с id courseId",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого курса не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/course/{courseId}/user/{userId}/batch/reviewed")
    public ResponseEntity<List<SolutionWithTaskShortResponseDto>> findReviewedSolutionsByCourseIdAndUserId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @PathVariable UUID userId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"SOLUTION_SUBMITTED_AT", "TASK_PUBLISHED_AT"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Такого пользователя не существует",
                        String.format("Ошибка поиска решений по курсу и id пользователя пользователем %s." +
                                " Пользователя %s не существует", principal.getEmail(), userId)
                ));

        checkPrincipalHasAccessToOtherOrElseThrow(principal, user, true, true);
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        return ResponseEntity.ok(
                solutionService.findSolutionsByCourseIdAndUserId(principal, courseId, userId, sortBy, false)
        );
    }

    @Operation(
            summary = "Получение Решений пользователя с id userId для задания с id taskId",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому этому пользователю"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Пользователь не загрузил решение к этому заданию"
                                        }
                                    """)
                    ))
    }
    )
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

        return ResponseEntity.ok(solutionService.findSolutionByTaskIdAndUserId(principal, taskId, userId));
    }

    @Operation(
            summary = "Получение решений участников курса с id courseId для всех заданий этого курса",
            description = "Требуется роль ADMIN или TUTOR.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому этому курсу"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого курса не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/course/{courseId}/batch")
    public ResponseEntity<List<SolutionsGroupByTaskShortResponseDto>> findSolutionsByCourseIdGroupByTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"TASK_PUBLISHED_AT"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy) {

        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(
                solutionService.findSolutionsByCourseIdGroupByTask(principal, courseId, sortBy)
        );
    }

    @Operation(
            summary = "Получение НЕПРОВЕРЕННЫХ решений участников курса с id courseId для всех заданий этого курса",
            description = "Требуется роль ADMIN или TUTOR.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому этому курсу"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого курса не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/course/{courseId}/batch/unreviewed")
    public ResponseEntity<List<SolutionsGroupByTaskShortResponseDto>> findUnreviewedSolutionsByCourseIdGroupByTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"TASK_PUBLISHED_AT"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy) {

        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(
                solutionService.findSolutionsByCourseIdGroupByTask(principal, courseId, sortBy, true)
        );
    }

    @Operation(
            summary = "Получение ПРОВЕРЕННЫХ решений участников курса с id courseId для всех заданий этого курса",
            description = "Требуется роль ADMIN или TUTOR.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление решениями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому этому курсу"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого курса не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/course/{courseId}/batch/reviewed")
    public ResponseEntity<List<SolutionsGroupByTaskShortResponseDto>> findReviewedSolutionsByCourseIdGroupByTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @Parameter(
                    description = "Поле для сортировки",
                    schema = @Schema(allowableValues = {"TASK_PUBLISHED_AT"})
            )
            @RequestParam(value = "sort_by", required = false) SortBy sortBy) {

        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(
                solutionService.findSolutionsByCourseIdGroupByTask(principal, courseId, sortBy,false)
        );
    }

    @Operation(
            summary = "Просмотр/скачивание файла с id fileId из решения с id solutionId",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Просмотр/скачивание файлов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Решение не найдено"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере при поиске файла",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{solutionId}/files/{fileId}")
    public ResponseEntity<Resource> downloadSolutionFile(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId,
            @PathVariable UUID fileId,
            @Parameter(
                    description = "Флаг. Если true - скачивание, false - просмотр"
            )
            @RequestParam(required = false, defaultValue = "false") boolean download
    ) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        Solution solution = solutionService.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Решение не найдено",
                        String.format("Ошибка при получении файла решения пользователем %s. " +
                                        "Решения с id %s не существует",
                                principal.getEmail(),
                                solutionId
                        )
                ));

        Content file = solution.getContent()
                .stream()
                .filter(c -> c.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new FileNotFoundEx(
                                "Ошибка. Такого файла не существует",
                                String.format("Ошибка при получении файла решения пользователем %s. " +
                                                "В решении %s не существует файла %s",
                                        principal.getEmail(),
                                        solutionId,
                                        fileId
                                )
                        )
                );

        Resource resource = solutionService.getSolutionFile(file.getPath());

        String disposition = download ? "attachment" : "inline";
        String contentType = solutionService.resolveContentType(file.getOriginalFileName());

        String encodedFileName = URLEncoder.encode(file.getOriginalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        String.format("%s; filename*=UTF-8''%s", disposition, encodedFileName)
                )
                .body(resource);
    }

    @Operation(
            summary = "Скачивание архива файлов по id решения",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Просмотр/скачивание файлов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Решение не найдено"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере при архивировании файлов",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{solutionId}/zip")
    public void downloadSolutionFilesZip(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId,
            HttpServletResponse response
    ) {
        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        Solution solution = solutionService.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Решение не найдено",
                        String.format("Ошибка при получении файлов решения пользователем %s. " +
                                        "Решения с id %s не существует",
                                principal.getEmail(),
                                solutionId
                        )
                ));

        List<Content> files = solution.getContent();

        String fileName = solution.getTask().getTitle() + "_" + solution.getUser().getSurname() + "_" +
                solution.getUser().getName().charAt(0);
        response.setContentType("application/zip");

        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (Content file : files) {
                Path filePath = Paths.get(basePath).resolve(file.getPath());
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    zipOut.putNextEntry(new ZipEntry(file.getOriginalFileName()));
                    Files.copy(filePath, zipOut);
                    zipOut.closeEntry();
                }
            }
        } catch (IOException ex) {
            throw new IOEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при архивировании файлов. Ошибка при архивировании файлов пользователем %s" +
                            " для решения %s", principal.getId(), solutionId)
            );
        }
    }

    @Operation(
            summary = "Добавление комментария к решению с указанным id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление комментариями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Поле text не может отсутствовать"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Конфликт версий данных",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Решение было изменено другим пользователем"
                                        }
                                    """)
                    )),
    }
    )
    @PostMapping("/{solutionId}/comments")
    public ResponseEntity<?> saveComment(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId,
            @Valid @RequestBody SolutionCommentCreateRequestDto request
    ) {

        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        solutionService.saveComment(principal, solutionId, request);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Получение комментария к решению с указанным id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление комментариями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому решению"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого решения не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{solutionId}/comments")
    public ResponseEntity<List<SolutionCommentResponseDto>> findCommentsBySolutionId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId
    ) {
        checkPrincipalHasAccessToSolutionOrElseThrow(principal, solutionId);

        return ResponseEntity.ok(solutionService.findCommentsBySolutionId(principal, solutionId));
    }

    @Operation(
            summary = "Удаление комментария с id commentId из решения с solutionId",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление комментариями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому комментарию"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Такого решения не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Конфликт версий данных",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Решение было изменено другим пользователем"
                                        }
                                    """)
                    )),
    }
    )
    @DeleteMapping("/{solutionId}/comments/{commentId}")
    public ResponseEntity<?> deleteCommentById(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID solutionId,
            @PathVariable UUID commentId
    ) {

        checkPrincipalHasAccessToComment(principal, solutionId, commentId);

        solutionService.deleteComment(principal, solutionId, commentId);

        return ResponseEntity.ok().build();
    }

    private void checkPrincipalHasAccessToComment(User principal, UUID solutionId, UUID commentId){
        if(!accessManager.isPrincipalHasAccessToSolutionComment(principal, solutionId, commentId)){
            throw new AccessDeniedEx(
                    "Ошибка доступа. У вас нет доступа к этому комментарию",
                    String.format("Отказ в доступе к комментарию к заданию. Пользователь %s не имеет доступа к комментарию %s",
                            principal.getEmail(), commentId)
            );
        }
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
