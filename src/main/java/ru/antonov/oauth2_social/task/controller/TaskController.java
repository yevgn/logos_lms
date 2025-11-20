package ru.antonov.oauth2_social.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.task.dto.TaskCreateRequestDto;
import ru.antonov.oauth2_social.task.dto.TaskResponseDto;

import ru.antonov.oauth2_social.task.dto.TaskShortResponseDto;
import ru.antonov.oauth2_social.task.dto.TaskUpdateRequestDto;
import ru.antonov.oauth2_social.common.Content;
import ru.antonov.oauth2_social.course.entity.Course;

import ru.antonov.oauth2_social.task.entity.Task;

import ru.antonov.oauth2_social.course.service.CourseService;
import ru.antonov.oauth2_social.task.service.TaskService;
import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;

import ru.antonov.oauth2_social.exception.FileNotFoundEx;
import ru.antonov.oauth2_social.exception.IOEx;
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
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TaskController {
    private final TaskService taskService;
    private final AccessManager accessManager;
    private final UserService userService;
    private final CourseService courseService;

    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    @Operation(
            summary = "Добавление задания к курсу",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление заданиями")
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
                                          "message": "Время дедлайна должно быть минимум на 1 час позже текущего"
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
                                          "message": "Ошибка. В данном курсе уже существует задание с таким названием"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере при сохранении файлов",
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
    @PostMapping(value = "/{courseId}", consumes = "multipart/form-data")
    public ResponseEntity<TaskResponseDto> addTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @Valid @ModelAttribute TaskCreateRequestDto request
            ){

        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, true);

        return ResponseEntity.ok(taskService.saveTask(principal, courseId, request));
    }

    @Operation(
            summary = "Обновление задания по id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление заданиями")
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
                                          "message": "Время дедлайна должно быть минимум на 1 час позже текущего"
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
                                          "message": "Ошибка. В данном задании уже существует файл с таким названием"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере при изменении файлов",
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
    @PatchMapping(value = "/{taskId}", consumes = "multipart/form-data")
    public ResponseEntity<TaskResponseDto> updateTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            @Valid @ModelAttribute TaskUpdateRequestDto request
    ){
        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Задание не найдено",
                        String.format("Ошибка при обновлении задания пользователем %s. " +
                                        "Задания с id %s не существует",
                                SecurityContextHolder.getContext().getAuthentication().getName(),
                                taskId
                        )
                ));

        checkPrincipalIsTaskAuthorOrCourseCreatorOrElseThrow(principal, task );

        return ResponseEntity.ok(taskService.updateTask(principal, task, request));
    }

    @Operation(
            summary = "Удаление задания по id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление заданиями")
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
                                          "message": "Ошибка. Задание не найдено"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "409",
                    description = "Конфликт версий данныз",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "409",
                                          "message": "Задание было удалено другим пользователем"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере при изменении файлов",
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
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId
    ){
        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Задание не найдено",
                        String.format("Ошибка при удалении задания пользователем %s. " +
                                        "Задания с id %s не существует",
                                principal.getEmail(),
                                taskId
                        )
                ));

        checkPrincipalIsTaskAuthorOrCourseCreatorOrElseThrow(principal, task);

        taskService.deleteTask(principal, task);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary =  "Получение информации о задании по id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление заданиями")
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
                                          "message": "Ошибка. Задание не найдено"
                                        }
                                    """)
                    ))
    }
    )
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> findTaskById(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId
    ){
        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        return ResponseEntity.ok(taskService.getTaskInfoById(principal, taskId));
    }

    @Operation(
            summary = "Получение информации о заданиях по id курса и id пользователя",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление заданиями")
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
                                          "message": "Ошибка доступа. Вы не имеете доступа к этому пользователю"
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
    @GetMapping("/course/{courseId}/user/{userId}")
    public ResponseEntity<List<TaskShortResponseDto>> findTasksByCourseIdAndUserId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId,
            @PathVariable UUID userId
    ){
        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Пользователь не найден",
                        String.format("Ошибка при поиске заданий пользователем %s. " +
                                        "Пользователь с id %s не существует",
                                principal.getEmail(),
                                userId
                        )
                ));

        checkPrincipalHasAccessToCourseOrElseThrow(principal, courseId, false, false);
        checkPrincipalHasAccessToOtherOrElseThrow(principal, user, true, true);

        return ResponseEntity.ok(taskService.getAllTaskInfoByCourseIdAndUserId(principal, courseId, userId));
    }


    @Operation(
            summary = "Получение информации о заданиях по id курса",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Управление заданиями")
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
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<TaskShortResponseDto>> findTasksByCourseId(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID courseId
    ){
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Курс не найден",
                        String.format("Ошибка при поиске заданий пользователем %s. " +
                                        "Курса с id %s не существует",
                                principal.getEmail(),
                                courseId
                        )
                ));

        return ResponseEntity.ok(taskService.getAllTaskInfoByCourseId(courseId));
    }

    @Operation(
            summary = "Просмотр/скачивание файл из заданий по fileId",
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
                                          "message": "Ошибка. Задание не найдено"
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
    @GetMapping("/{taskId}/files/{fileId}")
    public ResponseEntity<Resource> downloadTaskFile(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            @PathVariable UUID fileId,
            @Param(value = "Флаг. Если download = true, то файл будет скачан, если false - в режиме просмотра")
            @RequestParam(required = false, defaultValue = "false") boolean download
    ) {
        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Задание не найдено",
                        String.format("Ошибка при получении файла задания пользователем %s. " +
                                        "Задания с id %s не существует",
                                principal.getEmail(),
                                taskId
                        )
                ));

        Content file = task.getContent()
                .stream()
                .filter(c -> c.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new FileNotFoundEx(
                                "Ошибка. Такого файла не существует",
                                String.format("Ошибка при получении файла задания пользователем %s. " +
                                                "В задании %s не существует файла %s",
                                        principal.getEmail(),
                                        task.getId(),
                                        fileId
                                )
                        )
                );

        Resource resource = taskService.getTaskFile(file.getPath());

        String disposition = download ? "attachment" : "inline";
        String contentType = taskService.resolveContentType(file.getOriginalFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + file.getOriginalFileName() + "\""
                )
                .body(resource);
    }

    @Operation(
            summary = "Скачивание архива файлов по id задания",
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
                                          "message": "Ошибка. Задание не найдено"
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
    @GetMapping("/{taskId}/zip")
    public void downloadTaskFilesZip(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID taskId,
            HttpServletResponse response
    ) {
        checkPrincipalHasAccessToTaskOrElseThrow(principal, taskId);

        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Задание не найдено",
                        String.format("Ошибка при получении файла задания пользователем %s. " +
                                        "Задания с id %s не существует",
                                principal.getEmail(),
                                taskId
                        )
                ));

        List<Content> files = task.getContent();

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + task.getTitle() + "\"");

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
                            " для задания %s", principal.getId(), task.getId())
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

    private void checkPrincipalHasAccessToTaskOrElseThrow(User principal, UUID taskId){
        if(!accessManager.isPrincipalHasAccessToTask(principal, taskId)){
            throw new AccessDeniedEx(
                    "Ошибка доступа. У вас нет доступа к этому заданию",
                    String.format("Отказ в доступе к заданию. Пользователь %s не имеет доступа к заданию %s",
                            principal.getEmail(), taskId)
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