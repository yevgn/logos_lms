package ru.antonov.oauth2_social.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.course.dto.*;

import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.entity.CourseMaterial;
import ru.antonov.oauth2_social.common.Content;
import ru.antonov.oauth2_social.course.service.CourseService;

import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.exception.FileNotFoundEx;
import ru.antonov.oauth2_social.exception.IOEx;
import ru.antonov.oauth2_social.user.dto.DtoFactory;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.service.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CourseController {
    private final CourseService courseService;
    private final UserService userService;
    private final AccessManager accessManager;
    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    @Operation(
            summary = "Добавление курса по списку id пользователей",
            description = "Требуется роль TUTOR или ADMIN. В курс будут добавлены пользователи, найденные по списку id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. Вы не привязаны к учебному заведению"
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
                                          "message": "Поле name не может отсутствовать"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Дублирующиеся данные",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. В данном учебном заведении уже существует курс с таким названием"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/user-id-list")
    public ResponseEntity<CourseResponseDto> addCourse(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody CourseCreateWithUserIdListRequestDto request
    ) {
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        return ResponseEntity.ok(courseService.save(request, principal));
    }

    @Operation(
            summary = "Добавление курса по списку id учебных групп",
            description = "Требуется роль TUTOR или ADMIN. В курс будут добавлены пользователи из учебных групп из списка id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. Вы не привязаны к учебному заведению"
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
                                          "message": "Поле name не может отсутствовать"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Дублирующиеся данные",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. В данном учебном заведении уже существует курс с таким названием"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/group-id-list")
    public ResponseEntity<CourseResponseDto> addCourse(
            @AuthenticationPrincipal User principal,
            @RequestBody CourseCreateWithGroupIdListRequestDto request
    ) {
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        return ResponseEntity.ok(courseService.save(request, principal));
    }

    @Operation(
            summary = "Изменение названия курса",
            description = "Требуется роль TUTOR или ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому курсу"
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
                                          "message": "Параметр new_name не может отсутствовать"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Дублирующиеся данные",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. В данном учебном заведении уже существует курс с таким названием"
                                        }
                                    """)
                    ))
    }
    )
    @PatchMapping("/{courseId}/edit-name")
    public ResponseEntity<?> editCourseName(
            @AuthenticationPrincipal User principal,
            @PathVariable
            UUID courseId,
            @RequestParam("new_name")
            @NotBlank(message = "Поле new_name не может отсутствовать")
            @Size(min = 3, max = 255, message = "Длина поля new_name должна быть больше 2 и не превышать 255 символов")
            String newName
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, true, true);
        courseService.changeCourse(principal, courseId, newName);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Удаление курса по id",
            description = "Требуется роль TUTOR или ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому курсу"
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
                                          "message": "Параметр course_id не может отсутствовать"
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
                                          "message": "Курс был изменен. Повторите попытку"
                                        }
                                    """)
                    ))
    }
    )
    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, true, true);
        courseService.deleteById(courseId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Добавление пользователей в курс",
            description = "Требуется роль TUTOR или ADMIN. В курс будут добавлены пользователи, найденные по списку id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому курсу"
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
                                          "message": "Ошибка. Курса с указанным id не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Дублирующиеся данные",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. Один или несколько пользователей, которых вы пытаетесь
                                           добавить, уже состоят в этом курсе"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при отправке сообщения о добавлении пользователя в курс на SMTP-сервер",
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
    @PatchMapping("/{courseId}/add-users/user-id-list")
    public ResponseEntity<List<UserShortResponseDto>> addUsersToCourseByUserIdList(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestBody List<UUID> userIdList
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(courseService.addUsersToCourseByUserIdList(principal, courseId, userIdList, principal));
    }

    @Operation(
            summary = "Добавление пользователей в курс",
            description = "Требуется роль TUTOR или ADMIN. В курс будут добавлены пользователи из учебных групп по списку id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому курсу"
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
                                          "message": "Ошибка. Курса с указанным id не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Дублирующиеся данные",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. Один или несколько пользователей, которых вы пытаетесь
                                           добавить, уже состоят в этом курсе"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при отправке сообщения о добавлении пользователя в курс на SMTP-сервер",
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
    @PatchMapping("/{courseId}/add-users/group-id-list")
    public ResponseEntity<List<UserShortResponseDto>> addUsersToCourseByGroupIdList(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestBody List<UUID> groupIdList
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(courseService.addUsersToCourseByGroupIdList(principal, courseId, groupIdList, principal));
    }


    @Operation(
            summary = "Удаление пользователей из курса",
            description = "Требуется роль TUTOR или ADMIN. Из курса будут удалены пользователи по списку id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому курсу"
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
                                          "message": "Ошибка. Курса с указанным id не существует"
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
                                          "message": "Курс был изменен. Повторите попытку"
                                        }
                                    """)
                    ))
    }
    )
    @PatchMapping("/{courseId}/remove-users/user-id-list")
    public ResponseEntity<?> removeUsersFromCourseByUserIdList(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestBody List<UUID> userIdList
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        courseService.removeUsersFromCourseByUserIdList(courseId, userIdList);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Получении информации о курсе по id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому курсу"
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
                                          "message": "Ошибка. Курса с указанным id не существует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> getCourseInfo(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        return ResponseEntity.ok(courseService.getCourseInfoById(principal, courseId));
    }

    @Operation(
            summary = "Получение списка курсов по id учебного заведения",
            description = "Требуется роль ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому учебному заведению"
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
                                          "message": "Неправильный формат UUID для параметра institution_id"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<CourseResponseDto>> getCoursesByInstitutionId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID institutionId
    ) {
        checkPrincipalHasAccessToInstitutionOrElseThrow(principal, institutionId);

        return ResponseEntity.ok(courseService.findCoursesByInstitutionId(principal, institutionId));
    }

    @Operation(
            summary = "Получение списка курсов пользователя по его id",
            description = "Здесь возвращается список тех курсов," +
                    " членом которых является указанный пользователь",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому пользователю"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CourseShortResponseDto>> getCoursesByUserId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID userId
    ) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователя с указанным id не существует",
                        String.format("Ошибка при поиске курсов по id пользователя пользователем %s" +
                                ". Пользователя с id %s не существует", principal.getId(), userId)
                ));

        checkPrincipalHasAccessToOtherOrElseThrow(principal, user, true, true);

        return ResponseEntity.ok(courseService.findCoursesByUserId(principal, userId));
    }

    @Operation(
            summary = "Добавление учебных материалов в курс",
            description = "Требуется роль TUTOR или ADMIN",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому курсу"
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
                                          "message": "Неправильный формат UUID для параметра course_id"
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
                                          "message": "Курс был изменен. Повторите попытку"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при сохранении файлов на диск",
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
    @PostMapping(value = "/{courseId}/materials", consumes = "multipart/form-data")
    public ResponseEntity<CourseMaterialResponseDto> addCourseMaterialsToCourse(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @Valid @ModelAttribute CourseMaterialCreateRequestDto request
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(courseService.saveCourseMaterial(principal, courseId, request));
    }

    @Operation(
            summary = "Удаление учебного материала по id",
            description = "Требуется роль TUTOR или ADMIN. Удалить учебный материал может только создатель курса",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. У вас нет доступа к этому курсу"
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
                                          "message": "Ошибка. Учебный материал не найден"
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
                                          "message": "Учебный материал был изменен. Повторите попытку"
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
    @DeleteMapping("/{courseId}/materials")
    public ResponseEntity<?> deleteCourseMaterial(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestParam("course_material_id") UUID courseMaterialId
    ) {
        CourseMaterial courseMaterial = courseService.findCourseMaterialById(courseMaterialId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Учебный материал не найден",
                        String.format("Ошибка при удалении учебного материала пользователем %s. " +
                                        "Учебного материала с id %s не существует",
                                SecurityContextHolder.getContext().getAuthentication().getName(),
                                courseMaterialId
                        )
                ));

        checkPrincipalIsCourseMaterialAuthorOrCourseCreatorOrElseThrow(principal, courseMaterial);

        courseService.deleteCourseMaterial(principal, courseMaterial);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Обновление учебного материала по id",
            description = "Требуется роль TUTOR или ADMIN. Обновить материал может только тот преподаватель, который его выложил",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. Вы не являетесь автором этого учебного материала"
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
                                          "message": "Ошибка. Учебный материал не найден"
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
                                          "message": "Учебный материал был изменен. Повторите попытку"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при записи файлов/удалении файлов на диск/с диска",
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
    @PatchMapping(value = "/{courseId}/materials", consumes = "multipart/form-data")
    public ResponseEntity<CourseMaterialResponseDto> updateCourseMaterial(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestParam("course_material_id") UUID courseMaterialId,
            @Valid @ModelAttribute CourseMaterialUpdateRequestDto request
    ) {
        CourseMaterial courseMaterial = courseService.findCourseMaterialById(courseMaterialId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Учебный материал не найден",
                        String.format("Ошибка при обновлении учебного материала пользователем %s. " +
                                        "Учебного материала с id %s не существует",
                                SecurityContextHolder.getContext().getAuthentication().getName(),
                                courseMaterialId
                        )
                ));

        checkPrincipalIsCourseMaterialAuthorOrCourseCreatorOrElseThrow(principal, courseMaterial);

        return ResponseEntity.ok(courseService.updateCourseMaterial(principal, courseMaterial, request));
    }

    @Operation(
            summary = "Получение списка учебных материалов по id курса",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому курсу"
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
                                          "message": "Ошибка. Курс не найден"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{courseId}/materials")
    public ResponseEntity<List<CourseMaterialResponseDto>> findCourseMaterialsByCourseId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        return ResponseEntity.ok(courseService.findAllCourseMaterialsByCourseId(principal, courseId));
    }

    @Operation(
            summary = "Просмотр/скачивание файла из учебных материалов по fileId",
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому курсу"
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
                                          "message": "Ошибка. Учебный материал не найден"
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
    @GetMapping("/{courseId}/materials/{materialId}/files/{fileId}")
    public ResponseEntity<Resource> downloadCourseMaterialFile(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @PathVariable UUID materialId,
            @PathVariable UUID fileId,
            @Parameter(
                    description = "Флаг. Если true - скачивание, false - просмотр"
            )
            @RequestParam(required = false, defaultValue = "false") boolean download
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        CourseMaterial courseMaterial = courseService.findCourseMaterialById(materialId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Учебный материал не найден",
                        String.format("Ошибка при получении файла courseMaterial пользователем %s. " +
                                        "Учебного материала с id %s не существует",
                                SecurityContextHolder.getContext().getAuthentication().getName(),
                                materialId
                        )
                ));

        if (!courseId.equals(courseMaterial.getCourse().getId())) {
            throw new EntityNotFoundEx(
                    "Ошибка. В данном курсе нет такого учебного материала",
                    String.format("Ошибка при получении файла courseMaterial пользователем %s. " +
                                    "в курсе %s не существует учебного материала %s",
                            SecurityContextHolder.getContext().getAuthentication().getName(),
                            courseId, materialId
                    )
            );
        }

        Content file = courseMaterial.getContent()
                .stream()
                .filter(c -> c.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new FileNotFoundEx(
                                "Ошибка. Такого файла не существует",
                                String.format("Ошибка при получении файла courseMaterial пользователем %s. " +
                                                "В учебном материале %s не существует файла %s",
                                        SecurityContextHolder.getContext().getAuthentication().getName(),
                                        materialId,
                                        fileId
                                )
                        )
                );

        Resource resource = courseService.getCourseMaterialFile(file.getPath());

        String disposition = download ? "attachment" : "inline";
        String contentType = courseService.resolveContentType(file.getOriginalFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + file.getOriginalFileName() + "\""
                )
                .body(resource);
    }

    @Operation(
            summary = "Скачивание архива файлов по id учебного материала",
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому курсу"
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
                                          "message": "Ошибка. Учебный материал не найден"
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
    @GetMapping("/{courseId}/materials/{materialId}/zip")
    public void downloadCourseMaterialFilesZip(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @PathVariable UUID materialId,
            HttpServletResponse response
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        CourseMaterial courseMaterial = courseService.findCourseMaterialById(materialId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Учебный материал не найден",
                        String.format("Ошибка при получении файла courseMaterial пользователем %s. " +
                                        "Учебного материала с id %s не существует",
                                SecurityContextHolder.getContext().getAuthentication().getName(),
                                materialId
                        )
                ));

        if (!courseId.equals(courseMaterial.getCourse().getId())) {
            throw new EntityNotFoundEx(
                    "Ошибка. В данном курсе нет такого учебного материала",
                    String.format("Ошибка при получении файла courseMaterial пользователем %s. " +
                                    "в курсе %s не существует учебного материала %s",
                            SecurityContextHolder.getContext().getAuthentication().getName(),
                            courseId, materialId
                    )
            );
        }

        List<Content> files = courseMaterial.getContent();

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + courseMaterial.getTopic() + "\"");

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
                            " для учебного материала %s", principal.getId(), courseMaterial.getId())
            );
        }
    }

    @Operation(
            summary = "Получение информации о пользователях по id курса",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому курсу"
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
                                          "message": "параметр course_id отсутствует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{courseId}/users")
    public ResponseEntity<List<UserShortResponseDto>> findUsersByCourseId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);

        List<User> users = userService.findAllByCourseId(courseId);

        return ResponseEntity.ok(
                users.stream()
                        .map(ru.antonov.oauth2_social.user.dto.DtoFactory::makeUserShortResponseDto)
                        .toList()
        );
    }

    @Operation(
            summary = "Получение информации о пользователях" +
                    ", которые не состоят в курсе с указанным id",
            description = "В теле ответа будут пользователи все пользователи из учебного заведения, которые " +
                    "не состоят в курсе с указанным id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому учебному заведению"
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
                                          "message": "параметр institution_id отсутствует"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{courseId}/users/not")
    public ResponseEntity<List<UserShortResponseDto>> findUsersByCourseIdNot(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId
    ) {
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Такого курса не существует",
                        String.format("Ошибка при пользователей - не членов курса пользователем %s" +
                                ". Курса с id %s не существует", principal.getId(), courseId)
                ));

        List<User> users = userService.findAllByInstitutionIdNotInCourse(course.getInstitution().getId(), courseId);

        return ResponseEntity.ok(
                users.stream()
                        .map(DtoFactory::makeUserShortResponseDto)
                        .toList()
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

    private void checkPrincipalIsCourseMaterialAuthorOrCourseCreatorOrElseThrow(
            User principal, CourseMaterial courseMaterial
    ) {
        if (!(principal.equals(courseMaterial.getUser()) ||
                accessManager.isUserHasAccessToCourse(
                        principal, courseMaterial.getCourse().getId(), true, true)
        )
        ) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не являетесь создателем курса или автором учебного материала",
                    String.format(
                            "Ошибка доступа. Пользователь %s не является создателем курса %s или" +
                                    " автором материала %s",
                            principal.getId(), courseMaterial.getCourse().getId(), courseMaterial.getId()
                    )
            );
        }
    }

    private void checkPrincipalIsCourseMaterialAuthorOrElseThrow(User principal, CourseMaterial courseMaterial) {
        if (!principal.equals(courseMaterial.getUser())) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не являетесь автором этого учебного материала",
                    String.format(
                            "Ошибка в доступе к учебному материалу. Пользователь %s не является автором материала %S",
                            principal.getId(), courseMaterial.getId()
                    )
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

    private void checkPrincipalHasAccessToOtherOrElseThrow(User user, User other, boolean isNeedToHaveHigherPriority,
                                                      boolean isNeedToBeInOneInstitute) {
        if (Objects.equals(user.getId(), other.getId())) {
            return;
        }

        if (!accessManager.isUserHasAccessToOther(user, other, isNeedToHaveHigherPriority, isNeedToBeInOneInstitute)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не имеете доступа к этому пользователю",
                    String.format("Пользователю %s не разрешен доступ к пользователю %s", user.getEmail(), other.getEmail())
            );
        }
    }

    private void checkPrincipalHasAccessToInstitutionOrElseThrow(User user, UUID institutionId) {
        if (!accessManager.isUserHasAccessToInstitution(user, institutionId)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не имеете доступа к этому учебному заведению.",
                    String.format("Пользователю %s не разрешен доступ к учебному заведению %s", user.getEmail(), institutionId)
            );
        }
    }


}
