package ru.antonov.oauth2_social.task.service;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.common.Content;
import ru.antonov.oauth2_social.course.entity.*;
import ru.antonov.oauth2_social.course.exception.*;
import ru.antonov.oauth2_social.course.service.CourseLimitCounter;
import ru.antonov.oauth2_social.course.service.CourseService;
import ru.antonov.oauth2_social.common.FileService;
import ru.antonov.oauth2_social.solution.dto.SolutionCommentCreateRequestDto;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.solution.exception.SolutionCommentAmountLimitExceededEx;
import ru.antonov.oauth2_social.task.dto.*;
import ru.antonov.oauth2_social.task.repository.TaskRepository;
import ru.antonov.oauth2_social.exception.*;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.task.entity.TaskUser;
import ru.antonov.oauth2_social.task.entity.TaskUserKey;
import ru.antonov.oauth2_social.task.exception.TaskFileLimitExceededEx;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.service.UserService;

import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;

    private final UserService userService;
    private final CourseService courseService;
    private final CourseLimitCounter courseLimitCounter;
    private final FileService fileService;
    private final TaskEmailService taskEmailService;

    @Value("${spring.application.course-limit-params.max-task-and-material-file-amount-for-course}")
    private int maxTaskAndMaterialFileAmountForCourse;
    @Value("${spring.application.course-limit-params.max-file-amount-for-task}")
    private int maxFileAmountForTask;
    @Value("${spring.application.course-limit-params.max-comment-amount-for-task}")
    private int maxCommentAmountForTask;
    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    @Transactional(rollbackFor = Exception.class)
    public TaskResponseDto saveTask(User principal, UUID courseId, TaskCreateRequestDto request) {
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Курса с id %s не существует", courseId),
                        String.format("Ошибка при добавлении задания в курс пользователем %s. " +
                                "Курса с id %s не существует", principal.getId(), courseId)
                ));

        List<MultipartFile> files = request.getContent();

        if (courseLimitCounter.isTaskAndMaterialFileAmountForCourseExceedsLimit(course.getId(), files.size())) {
            throw new TaskAndCourseMaterialFileLimitForCourseExceededEx(
                    "Ошибка при загрузке файлов. Превышено максимально допустимое число учебных " +
                            "файлов для этого курса",
                    String.format("Ошибка при загрузке файлов. " +
                            "Превышено максимально допустимое количество учебных файлов для курса с %s id", course.getId())
            );
        }

        UUID taskId = UUID.randomUUID();

        List<FileService.FileInfo> contentInfoList = files.stream()
                .map(f -> {
                    UUID fileId = UUID.randomUUID();
                    Path path = fileService.generateTaskFilePath(
                            course.getId(), taskId, fileId, fileService.getFileExtension(f.getOriginalFilename())
                    );
                    return new FileService.FileInfo(path, f.getOriginalFilename(), f, fileId);
                })
                .toList();

        List<Content> content = contentInfoList.stream()
                .map(ci -> Content
                        .builder()
                        .id(ci.getId())
                        .originalFileName(ci.getFileName())
                        .path(ci.getPath().toString())
                        .build()
                )
                .toList();

        Task task = Task
                .builder()
                .id(taskId)
                .title(request.getTitle())
                .description(request.getDescription())
                .publishedAt(LocalDateTime.now())
                .toSubmitAt(request.getToSubmitAt())
                .creator(principal)
                .content(content)
                .isAssessed(request.isAssessed())
                .isForEveryone(request.isForEveryone())
                .course(course)
                .build();

        List<User> targetUsers = List.of();

        try {
            taskRepository.saveAndFlush(task);
        } catch (DataIntegrityViolationException ex) {
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique") &&
                    sqlEx.getMessage().toLowerCase().contains("title_course_id")) {
                message = "Ошибка. В данном курсе уже существует задание с таким названием";
                debugMessage = String.format("Ошибка при добавлении задания. " +
                        "В курсе %s уже существует задание с названием %s", course.getId(), request.getTitle());
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }

        if (!request.isForEveryone()) {
            List<User> targetUsersInCourse = courseService
                    .findAllUsersByCourseIdAndUserIdIn(courseId, request.getTargetUsersIdList())
                    .stream()
                    .filter(u -> u.getRole().equals(Role.STUDENT))
                    .toList();

            Set<TaskUser> taskTargetUsers = targetUsersInCourse.stream()
                    .map(u ->
                            TaskUser.builder()
                                    .id(
                                            TaskUserKey.builder().userId(u.getId()).taskId(taskId).build()
                                    )
                                    .user(u)
                                    .task(task)
                                    .build()
                    ).collect(Collectors.toSet());

            task.setTaskUsers(taskTargetUsers);
            taskRepository.saveAndFlush(task);

            targetUsers = taskTargetUsers.stream().map(TaskUser::getUser).toList();
        } else {
            targetUsers = task.getCourse().getCourseUsers()
                    .stream()
                    .map(CourseUser::getUser)
                    .filter(u -> u.getRole().equals(Role.STUDENT))
                    .toList();
        }

        fileService.uploadFiles(contentInfoList);

        targetUsers.forEach(u -> taskEmailService.sendTaskUploadedNotification(u, task));

        return ru.antonov.oauth2_social.task.dto.DtoFactory.makeTaskResponseDto(task, targetUsers);
    }

    public Task save(Task task) {
        return taskRepository.saveAndFlush(task);
    }

    public Optional<Task> findById(UUID taskId) {
        return taskRepository.findById(taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskResponseDto updateTask(
            User principal, Task task, TaskUpdateRequestDto request
    ) {
        boolean isTaskUpdated = false;
        List<FileService.FileInfo> filesToWriteList = new ArrayList<>();
        List<Path> pathToDeleteList = new ArrayList<>();

        String newTitle = request.getNewTitle();
        if (newTitle != null && !newTitle.isBlank()) {
            task.setTitle(newTitle);
            isTaskUpdated = true;
        }

        String newDescription = request.getNewDescription();
        if (newDescription != null && !newDescription.isBlank()) {
            task.setDescription(newDescription);
            isTaskUpdated = true;
        }

        LocalDateTime newToSubmitAt = request.getNewToSubmitAt();
        if (newToSubmitAt != null) {
            task.setToSubmitAt(newToSubmitAt);
            isTaskUpdated = true;
        }

        // удалить файлы
        List<UUID> toDeleteList = request.getToDeleteList();

        if (toDeleteList != null && !toDeleteList.isEmpty()) {
            List<Content> content = task.getContent();
            content = content.stream()
                    .filter(c -> {
                        boolean isToDeleteAndSkip = toDeleteList.contains(c.getId());
                        if (isToDeleteAndSkip) {
                            pathToDeleteList.add(Path.of(c.getPath()));
                        }
                        return !isToDeleteAndSkip;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
            task.setContent(content);
            isTaskUpdated = true;
        }

        if (request.getContent() != null && !request.getContent().isEmpty()) {
            List<MultipartFile> newContent = request.getContent();
            newContent.forEach(c -> {
                for (Content content : task.getContent()) {
                    if (content.getOriginalFileName().equals(c.getOriginalFilename())) {
                        throw new FileDuplicatedEx(
                                String.format("Файл с именем %s уже существует в этом задании",
                                        c.getOriginalFilename()),
                                String.format("Ошибка при обновлении задания %s. Пользователь %s пытается " +
                                                "загрузить файл с именем %s, который уже существует",
                                        task.getId(), principal.getId(), c.getOriginalFilename())
                        );
                    }
                }
            });

            if (courseLimitCounter.isTaskAndMaterialFileAmountForCourseExceedsLimit(
                    task.getCourse().getId(), newContent.size() - pathToDeleteList.size()
            )) {
                throw new TaskAndCourseMaterialFileLimitForCourseExceededEx(
                        String.format("Ошибка при загрузке файлов. Превышено максимально " +
                                        "допустимое число файлов для этого курса - %s",
                                maxTaskAndMaterialFileAmountForCourse
                        ),
                        String.format("Ошибка при загрузке файлов. " +
                                        "Превышено максимально допустимое количество учебных файлов для курса с %s id",
                                task.getCourse().getId()
                        )
                );
            } else if (courseLimitCounter.isFileAmountForTaskExceedsLimit(
                    task.getCourse().getId(), task.getId(), newContent.size() - pathToDeleteList.size()
            )) {
                throw new TaskFileLimitExceededEx(
                        String.format("Ошибка при загрузке файлов. Превышено максимально допустимое число файлов" +
                                " в рамках одного задания - %d", maxFileAmountForTask),
                        String.format("Ошибка при загрузке файлов пользователем %s в задание %s. " +
                                        "Превышено максимально допустимое число файлов в рамках одного задания",
                                principal.getId(), task.getId())
                );
            }

            List<FileService.FileInfo> contentInfoList = newContent.stream()
                    .map(f -> {
                        UUID fileId = UUID.randomUUID();
                        Path path = fileService.generateTaskFilePath(
                                task.getCourse().getId(), task.getId(), fileId,
                                fileService.getFileExtension(f.getOriginalFilename())
                        );
                        return new FileService.FileInfo(path, f.getOriginalFilename(), f, fileId);
                    })
                    .toList();

            filesToWriteList = contentInfoList;

            List<Content> taskContent = contentInfoList.stream()
                    .map(ci -> Content
                            .builder()
                            .id(ci.getId())
                            .originalFileName(ci.getFileName())
                            .path(ci.getPath().toString())
                            .build()
                    )
                    .collect(Collectors.toCollection(ArrayList::new));

            task.addContent(taskContent);
            isTaskUpdated = true;
        }

        if (isTaskUpdated) {
            task.setLastChangedAt(LocalDateTime.now());

            try {
                taskRepository.saveAndFlush(task);
            } catch (OptimisticLockException ex) {
                throw new EntityLockEx(
                        "Задание было изменено другим пользователем. Повторите попытку",
                        String.format("Ошибка при обновлении задания пользователем %s. Задание %s было " +
                                "изменено другим пользователем", principal.getId(), task.getId())
                );
            } catch (DataIntegrityViolationException ex) {
                Throwable root = NestedExceptionUtils.getRootCause(ex);
                String message;
                String debugMessage;

                if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique") &&
                        sqlEx.getMessage().toLowerCase().contains("title_course_id")) {
                    message = "Ошибка. В данном курсе уже существует задание с таким названием";
                    debugMessage = String.format("Ошибка при добавлении задания. " +
                            "В курсе %s уже существует задание с названием %s", task.getCourse().getId(), request.getNewTitle());
                } else {
                    message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                    debugMessage = (root != null ? root.getMessage() : ex.getMessage());
                }

                throw new DBConstraintViolationEx(message, debugMessage);
            }

            pathToDeleteList.forEach(fileService::deleteFile);
            fileService.uploadFiles(filesToWriteList);
        }

        List<User> targetUsers = new ArrayList<>();
        if (task.isForEveryone()) {
            targetUsers = userService.findAllByCourseId(task.getCourse().getId());
        } else {
            targetUsers = task.getTaskUsers()
                    .stream()
                    .map(TaskUser::getUser)
                    .toList();
        }

        return ru.antonov.oauth2_social.task.dto.DtoFactory.makeTaskResponseDto(task, targetUsers);
    }

    public void deleteTask(User principal, Task task) {
        List<Content> content = task.getContent();
        try {
            taskRepository.delete(task);
        } catch (OptimisticLockException ex) {
            throw new EntityLockEx(
                    "Задание было изменено другим пользователем",
                    String.format("Ошибка при удалении задания пользователем %s. Задание %s было " +
                            "изменено другим пользователем", principal.getId(), task.getId())
            );
        }

        String pathStr = content.get(0).getPath();
        Path filePath = Paths.get(pathStr);
        Path dirPath = filePath.getParent();

        fileService.deleteDirectory(dirPath);
    }

    public TaskResponseDto getTaskInfoById(User principal, UUID taskId) {
        Task task = taskRepository.findByIdWithTaskUsers(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Задания с id %s не существует", taskId),
                        String.format("Ошибка при получении информации о задании пользователем %s. " +
                                "Задания с id %s не существует", principal.getEmail(), taskId)
                ));

        List<User> targetUsers = new ArrayList<>();
        if (task.isForEveryone()) {
            targetUsers = userService.findAllByCourseId(task.getCourse().getId())
                    .stream()
                    .filter(u -> u.getRole().equals(Role.STUDENT))
                    .toList();
        } else {
            targetUsers = task.getTaskUsers()
                    .stream()
                    .map(TaskUser::getUser)
                    .toList();
        }

        return ru.antonov.oauth2_social.task.dto.DtoFactory.makeTaskResponseDto(task, targetUsers);
    }

    public List<Task> findAllByCourseId(UUID courseId) {
        return taskRepository.findAllByCourseId(courseId);
    }

    public List<TaskShortResponseDto> getAllTaskInfoByCourseIdAndUserId(User principal, UUID courseId, UUID userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Пользователь не найден",
                        String.format("Ошибка при поиске заданий пользователем %s. " +
                                        "Пользователь с id %s не существует",
                                principal.getEmail(),
                                userId
                        )
                ));

        if (!courseService.isUserJoinedCourse(courseId, userId)) {
            throw new UserNotInCourseEx(
                    "Данный пользователь не состоит в указанном курсе",
                    String.format("Ошибка при поиске заданий пользователем %s. " +
                                    "Пользователь с id %s не состоит в курсе %s",
                            principal.getEmail(),
                            userId,
                            courseId
                    )
            );
        }

        if (!user.getRole().equals(Role.STUDENT)) {
            throw new IllegalArgumentEx(
                    "Ошибка. Указанный вами пользователь не является студентом",
                    String.format("Ошибка при поиске заданий пользователем %s. Пользователь %s, чьи задания" +
                            " пытаются найти, не является студентом", principal.getEmail(), userId)
            );
        }

        List<Task> commonTasks = findAllCommonTasksByCourseId(courseId);

        List<Task> targetTasks = findAllTargetTasksByCourseIdAndUserId(courseId, userId);

        Stream<Task> allUserTasksStream = Stream.concat(commonTasks.stream(), targetTasks.stream());

        return allUserTasksStream
                .map(ru.antonov.oauth2_social.task.dto.DtoFactory::makeTaskShortResponseDto)
                .toList();
    }

    public List<Task> findAllCommonTasksByCourseId(UUID courseId){
        return taskRepository.findAllCommonTasksByCourseId(courseId);
    }

    public List<Task> findAllTargetTasksByCourseIdAndUserId(UUID courseId, UUID userId){
        return taskRepository.findAllTargetTasksByCourseIdAndUserId(courseId, userId);
    }

    public List<Task> findAllTargetTasksByCourseId(UUID courseId){
        return taskRepository.findAllTargetTasksByCourseId(courseId);
    }

    public List<TaskShortResponseDto> getAllTaskInfoByCourseId(UUID courseId) {
        List<Task> courseTasks = taskRepository.findAllByCourseId(courseId);

        return courseTasks.stream()
                .map(DtoFactory::makeTaskShortResponseDto)
                .toList();
    }

    public Resource getTaskFile(String path){
        Path absPath = Paths.get(basePath).resolve(path);

        try {
            Resource resource = new UrlResource(absPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Файл не найден");
            }

            return resource;
        } catch (MalformedURLException ex){
            throw new IllegalArgumentException("Ошибка на сервере");
        }
    }

    public String resolveContentType(String fileName) {
        String contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return contentType;
    }

    public Optional<Task> findByIdWithSolutions(UUID taskId){
        return taskRepository.findByIdWithSolutions(taskId);
    }

    public void saveComment(User principal, UUID taskId, TaskCommentCreateRequestDto request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого задания не существует",
                        String.format("Ошибка при загрузке комментария к заданию пользователем %s. " +
                                "Задания %s не существует", principal.getEmail(), taskId)
                ));

        if (courseLimitCounter.isCommentAmountForTaskExceedsLimit(taskId, 1)) {
            throw new SolutionCommentAmountLimitExceededEx(
                    String.format("Ошибка. Превышено максимальное число комментариев для задания - %s",
                            maxCommentAmountForTask),
                    String.format("Ошибка добавления комментария к заданию %s пользователем %s. Превышен лимит",
                            taskId, principal.getId())
            );
        }

        Task.TaskComment comment = Task.TaskComment.builder()
                .id(UUID.randomUUID())
                .userId(principal.getId())
                .text(request.getText())
                .publishedAt(LocalDateTime.now())
                .build();

        task.addComments(List.of(comment));

        try {
            taskRepository.save(task);
        } catch (OptimisticLockException ex) {
            throw new EntityLockEx(
                    "Ошибка. Задание было изменено. Повторите попытку",
                    String.format("Ошибка при добавлении комментария к заданию %s пользователем %s. Задание было " +
                            "изменено другим пользователем", task.getId(), principal.getId())
            );
        }
    }

    public List<TaskCommentResponseDto> findCommentsByTaskId(User principal, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого задания не существует",
                        String.format("Ошибка при поиске комментариев к заданию пользователем %s. " +
                                "Задания %s не существует", principal.getEmail(), taskId)
                ));

        return task.getComments().stream().map(c -> {
                    User author = userService.findById(c.getUserId()).orElse(null);
                    return DtoFactory.makeTaskCommentResponseDto(c, author);
                })
                .toList();
    }

    public void deleteComment(User principal, UUID taskId, UUID commentId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого задания не существует",
                        String.format("Ошибка при удалении комментария к заданию пользователем %s. " +
                                "Задания %s не существует", principal.getEmail(), taskId)
                ));

        List<Task.TaskComment> comments = task.getComments();

        task.setComments(
                comments.
                        stream()
                        .filter(s -> !s.getId().equals(commentId))
                        .toList()
        );

        try {
            taskRepository.save(task);
        } catch (OptimisticLockException ex) {
            throw new EntityLockEx(
                    "Ошибка. Задание было изменено. Повторите попытку",
                    String.format("Ошибка при удалении комментария к заданию %s пользователем %s. Решение было " +
                            "изменено другим пользователем", task.getId(), principal.getId())
            );
        }
    }
}
