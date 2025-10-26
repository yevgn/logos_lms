package ru.antonov.oauth2_social.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import ru.antonov.oauth2_social.course.service.CourseService;

import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.user.dto.DtoFactory;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CourseController {
    private final CourseService courseService;
    private final UserService userService;
    private final AccessManager accessManager;

    @Operation(
            summary = "Добавление курса по списку id пользователей",
            description = "Требуется роль TUTOR. В курс будут добавлены пользователи, найденные по списку id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
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
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. В данном учебном заведении уже существует курс с таким названием"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при отправке сообщения о добавлении пользователя в курс на SMTP-сервер",
                    content = @Content(
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
    @PostMapping("/user-id-list")
    public ResponseEntity<CourseResponseDto> addCourse(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody CourseCreateWithUserIdListRequestDto request
    ){
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        return ResponseEntity.ok(courseService.save(request, principal));
    }

    @Operation(
            summary = "Добавление курса по списку id учебных групп",
            description = "Требуется роль TUTOR. В курс будут добавлены пользователи из учебных групп из списка id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
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
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Ошибка. В данном учебном заведении уже существует курс с таким названием"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при отправке сообщения о добавлении пользователя в курс на SMTP-сервер",
                    content = @Content(
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
    @PostMapping("/group-id-list")
    public ResponseEntity<CourseResponseDto> addCourse(
            @AuthenticationPrincipal User principal,
            @RequestBody CourseCreateWithGroupIdListRequestDto request
    ){
        checkPrincipalIsAttachedToInstitutionOrElseThrow(principal);

        return ResponseEntity.ok(courseService.save(request, principal));
    }

    @Operation(
            summary = "Изменение названия курса",
            description = "Требуется роль TUTOR",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
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
                    content = @Content(
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
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, true, true);
        courseService.changeCourse(courseId, newName);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Удаление курса по id",
            description = "Требуется роль TUTOR",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Параметр course_id не может отсутствовать"
                                        }
                                    """)
                    ))
    }
    )
    @DeleteMapping
    public ResponseEntity<?> deleteCourse(
            @AuthenticationPrincipal User principal,
            @RequestParam("course_id") UUID courseId
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, true, true);
        courseService.deleteById(courseId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Добавление пользователей в курс",
            description = "Требуется роль TUTOR. В курс будут добавлены пользователи, найденные по списку id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Курса с указанным id не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при отправке сообщения о добавлении пользователя в курс на SMTP-сервер",
                    content = @Content(
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
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(courseService.addUsersToCourseByUserIdList(courseId, userIdList, principal));
    }

    @Operation(
            summary = "Добавление пользователей в курс",
            description = "Требуется роль TUTOR. В курс будут добавлены пользователи из учебных групп по списку id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ошибка. Курса с указанным id не существует"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при отправке сообщения о добавлении пользователя в курс на SMTP-сервер",
                    content = @Content(
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
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(courseService.addUsersToCourseByGroupIdList(courseId, groupIdList, principal));
    }

    @Operation(
            summary = "Удаление пользователей из курса",
            description = "Требуется роль TUTOR. Из курса будут удалены пользователи по списку id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
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

    @Operation(
            summary = "Получении информации о курсе по id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
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
    @GetMapping
    public ResponseEntity<CourseResponseDto> getCourseInfo(
            @AuthenticationPrincipal User principal,
            @RequestParam("course_id") UUID courseId
    ){
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
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
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
    @GetMapping("/institution")
    public ResponseEntity<List<CourseResponseDto>> getCoursesByInstitutionId(
            @AuthenticationPrincipal User principal,
            @RequestParam("institution_id") UUID institutionId
    ){
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
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. У вас нет доступа к этому учебному заведению"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/user")
    public ResponseEntity<List<CourseResponseDto>> getCoursesByUserId(
            @AuthenticationPrincipal User principal
    ){
        return ResponseEntity.ok(courseService.findCoursesByUser(principal));
    }

    @Operation(
            summary = "Добавление учебных материалов в курс",
            description = "Требуется роль TUTOR",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление курсами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Неправильный формат UUID для параметра institution_id"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка при сохранении файлов на диск",
                    content = @Content(
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
    @PostMapping(value = "/{courseId}/materials", consumes = "multipart/form-data" )
    public ResponseEntity<CourseMaterialResponseDto> addCourseMaterialsToCourse(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @Valid @ModelAttribute CourseMaterialCreateRequestDto request
    ){
        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(courseService.saveCourseMaterial(principal, courseId, request));
    }

    @DeleteMapping("/{courseId}/materials")
    public ResponseEntity<?> deleteCourseMaterialById(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @RequestParam("course_material_id") UUID id
    ){
        // todo УДАЛИТЬ
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Получение информации о пользователях по id курса",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
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
    @GetMapping("/users")
    public ResponseEntity<List<UserShortResponseDto>> findUsersByCourseId(
            @AuthenticationPrincipal User principal,
            @RequestParam("course_id") UUID courseId
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
            summary = "Получение информации о пользователях из учебного заведения с указанным id, " +
            "которые не состоят в курсе с указанным id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление пользователями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Нет доступа",
                    content = @Content(
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
                    content = @Content(
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
    @GetMapping("/users/not")
    public ResponseEntity<List<UserShortResponseDto>> findUsersByCourseIdNot(
            @AuthenticationPrincipal User principal,
            @RequestParam("institution_id") UUID institutionId,
            @RequestParam("course_id") UUID courseId
    ) {

        checkUserHasAccessToInstitutionOrElseThrow(principal, institutionId);

        List<User> users = userService.findAllByInstitutionIdNotInCourse(institutionId, courseId);

        return ResponseEntity.ok(
                users.stream()
                        .map(DtoFactory::makeUserShortResponseDto)
                        .toList()
        );
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

    private void checkPrincipalHasAccessToInstitutionOrElseThrow(User user, UUID institutionId) {
        if (!accessManager.isUserHasAccessToInstitution(user, institutionId)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не имеете доступа к этому учебному заведению.",
                    String.format("Пользователю %s не разрешен доступ к учебному заведению %s", user.getEmail(), institutionId)
            );
        }
    }

    private void checkUserHasAccessToInstitutionOrElseThrow(User user, UUID institutionId) {
        if (!accessManager.isUserHasAccessToInstitution(user, institutionId)) {
            throw new AccessDeniedEx(
                    "Ошибка доступа. Вы не имеете доступа к этому учебному заведению.",
                    String.format("Пользователю %s не разрешен доступ к учебному заведению %s", user.getEmail(), institutionId)
            );
        }
    }

}
