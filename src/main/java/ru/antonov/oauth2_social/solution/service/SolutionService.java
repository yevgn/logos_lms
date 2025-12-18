package ru.antonov.oauth2_social.solution.service;

import jakarta.persistence.OptimisticLockException;
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
import ru.antonov.oauth2_social.common.FileService;
import ru.antonov.oauth2_social.common.exception.*;
import ru.antonov.oauth2_social.course.service.CourseLimitCounter;
import ru.antonov.oauth2_social.solution.common.SortBy;
import ru.antonov.oauth2_social.solution.repository.SolutionRepository;
import ru.antonov.oauth2_social.solution.dto.*;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.solution.entity.SolutionStatus;
import ru.antonov.oauth2_social.solution.exception.*;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.task.service.TaskService;
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
public class SolutionService {
    private final SolutionRepository solutionRepository;

    private final TaskService taskService;
    private final UserService userService;

    private final SolutionEmailService solutionEmailService;
    private final CourseLimitCounter courseLimitCounter;
    private final FileService fileService;

    @Value("${spring.application.course-limit-params.max-solution-file-amount-for-course}")
    private int maxSolutionFileAmountForCourse;
    @Value("${spring.application.course-limit-params.max-file-amount-for-solution}")
    private int maxFileAmountForSolution;
    @Value("${spring.application.course-limit-params.max-comment-amount-for-solution}")
    private int maxCommentAmountForSolution;
    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    @Transactional(rollbackFor = Exception.class)
    public SolutionResponseDto saveSolution(User principal, UUID taskId, SolutionCreateRequestDto request) {
        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Задания с id %s не существует", taskId),
                        String.format("Ошибка при загрузке решения пользователем %s. " +
                                "Задания с id %s не существует", principal.getId(), taskId)
                ));

        List<MultipartFile> files = request.getContent();
        UUID courseId = task.getCourse().getId();

        if (courseLimitCounter.isSolutionFileAmountForCourseExceedsLimit(courseId, files.size())) {
            throw new SolutionFileLimitForCourseExceededEx(
                    "Ошибка при загрузке файлов. Превышено максимально допустимое число файлов с решениями " +
                            "для этого курса",
                    String.format("Ошибка при загрузке файлов пользователем %s. " +
                                    "Превышено максимально допустимое количество файлов с решениями для курса с %s id",
                            principal.getId(), courseId)
            );
        }

        UUID solutionId = UUID.randomUUID();

        List<FileService.FileInfo> contentInfoList = files.stream()
                .map(f -> {
                    UUID fileId = UUID.randomUUID();
                    Path path = fileService.generateSolutionFilePath(
                            courseId, taskId, solutionId, fileId, fileService.getFileExtension(f.getOriginalFilename())
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

        Solution solution = Solution
                .builder()
                .id(solutionId)
                .submittedAt(LocalDateTime.now())
                .content(content)
                .user(principal)
                .task(task)
                .status(calculateSolutionStatus(task))
                .build();

        try {
            solutionRepository.saveAndFlush(solution);
        } catch (DataIntegrityViolationException ex) {
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique") &&
                    sqlEx.getMessage().toLowerCase().contains("user_id_task_id")) {
                message = "Ошибка. Вы уже загрузили решение для этого задания.";
                debugMessage = String.format("Ошибка при добавлении решения. " +
                        "Пользователь %s уже добавил решение к заданию %s", principal.getId(), taskId);
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }
            throw new DBConstraintViolationEx(message, debugMessage);
        }

        Path solutionCataloguePath = Paths.get(
                basePath, task.getCourse().getId().toString(), "tasks", task.getId().toString(), "solutions",
                solutionId.toString()
        );

        fileService.createDirectory(solutionCataloguePath);

        fileService.uploadFiles(contentInfoList);

        return DtoFactory.makeSolutionResponseDto(solution);
    }
//
//    public void revokeSolution(User principal, UUID solutionId) {
//        Solution solution = solutionRepository.findById(solutionId)
//                .orElseThrow(() -> new EntityNotFoundEx(
//                        String.format("Ошибка. Решения с id %s не существует", solutionId),
//                        String.format("Ошибка при отзыве решения пользователем %s. " +
//                                "Решения с id %s не существует", principal.getEmail(), solutionId)
//                ));
//
//        solution.setSubmittedAt(null);
//        solution.set
//        try {
//            solutionRepository.save(solution);
//        } catch (OptimisticLockException ex) {
//            throw new EntityLockEx(
//                    "Решение было изменено. Повторите попытку",
//                    String.format("Ошибка при обновлении решения пользователем %s. Решение %s было " +
//                            "изменено", principal.getId(), solutionId)
//            );
//        }
//    }

    @Transactional(rollbackFor = Exception.class)
    public SolutionResponseDto updateSolution(User principal, UUID solutionId, SolutionUpdateRequestDto request) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Решения с id %s не существует", solutionId),
                        String.format("Ошибка при обновлении решения пользователем %s. " +
                                "Решения с id %s не существует", principal.getId(), solutionId)
                ));

        boolean isSolutionUpdated = false;
        List<FileService.FileInfo> filesToWriteList = new ArrayList<>();
        List<Path> pathToDeleteList = new ArrayList<>();

        // удалить файлы
        List<UUID> toDeleteList = request.getToDeleteList();

        if (toDeleteList != null && !toDeleteList.isEmpty()) {
            List<Content> content = solution.getContent();
            content = content.stream()
                    .filter(c -> {
                        boolean isToDeleteAndSkip = toDeleteList.contains(c.getId());
                        if (isToDeleteAndSkip) {
                            pathToDeleteList.add(Path.of(c.getPath()));
                        }
                        return !isToDeleteAndSkip;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
            solution.setContent(content);
            isSolutionUpdated = true;
        }

        if (request.getContent() != null && !request.getContent().isEmpty()) {
            List<MultipartFile> newContent = request.getContent();
            newContent.forEach(c -> {
                for (Content content : solution.getContent()) {
                    if (content.getOriginalFileName().equals(c.getOriginalFilename())) {
                        throw new FileDuplicatedEx(
                                String.format("Файл с именем %s уже существует в этом решении",
                                        c.getOriginalFilename()),
                                String.format("Ошибка при обновлении решения %s. Пользователь %s пытается " +
                                                "загрузить файл с именем %s, который уже существует",
                                        solution.getId(), principal.getId(), c.getOriginalFilename())
                        );
                    }
                }
            });

            List<FileService.FileInfo> contentInfoList = newContent.stream()
                    .map(f -> {
                        UUID fileId = UUID.randomUUID();
                        Path path = fileService.generateSolutionFilePath(
                                solution.getTask().getCourse().getId(), solution.getTask().getId(), solution.getId(), fileId,
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

            solution.addContent(taskContent);
            isSolutionUpdated = true;
        }

        if (isSolutionUpdated) {
            solution.setStatus(calculateSolutionStatus(solution.getTask()));
            solution.setSubmittedAt(LocalDateTime.now());

            try {
                solutionRepository.saveAndFlush(solution);
            } catch (OptimisticLockException ex) {
                throw new EntityLockEx(
                        "Решение было изменено. Повторите попытку",
                        String.format("Ошибка при обновлении решения пользователем %s. Решение %s было " +
                                "изменено другим пользователем", principal.getId(), solution.getId())
                );
            }

            pathToDeleteList.forEach(fileService::deleteFile);

            if (courseLimitCounter.isSolutionFileAmountForCourseExceedsLimit(
                    solution.getTask().getCourse().getId(), filesToWriteList.size())) {
                throw new SolutionFileLimitForCourseExceededEx(
                        String.format("Ошибка при загрузке файлов. Превышено максимально " +
                                        "допустимое число файлов для этого курса - %s",
                                maxSolutionFileAmountForCourse
                        ),
                        String.format("Ошибка при загрузке файлов. " +
                                        "Превышено максимально допустимое количество файлов с решениями для курса с %s id",
                                solution.getTask().getCourse().getId()
                        )
                );
            } else if (courseLimitCounter.isFileAmountForSolutionExceedsLimit(
                    solution.getTask().getCourse().getId(), solution.getTask().getId(), solution.getId(), filesToWriteList.size()
            )) {
                throw new SolutionFileLimitExceededEx(
                        String.format("Ошибка при загрузке файлов. Превышено максимально допустимое число файлов" +
                                " в рамках одного решения - %d", maxFileAmountForSolution),
                        String.format("Ошибка при загрузке файлов пользователем %s в решение %s. " +
                                        "Превышено максимально допустимое число файлов в рамках одного решения",
                                principal.getId(), solution.getId())
                );
            }

            try {
                fileService.uploadFiles(filesToWriteList);
            } catch (IllegalArgumentException ex) {
                throw new EmptyFileEx(
                        "Ошибка. Вы пытаетесь загрузить пустой файл",
                        String.format("Ошибка при обновлении решения %s. Пользователь %s пытается " +
                                "загрузить пустой файл", solution.getId(), principal.getId())
                );
            }
        }

        return DtoFactory.makeSolutionResponseDto(solution);
    }

    public void deleteSolution(User principal, UUID solutionId) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого решения не существует",
                        String.format("Ошибка при удалении решения пользователем %s. " +
                                "Решения с id %s не существует", principal.getEmail(), solutionId)
                ));

        List<Content> content = solution.getContent();
        try {
            solutionRepository.delete(solution);
        } catch (OptimisticLockException ex) {
            throw new EntityLockEx(
                    "Ошибка. Решение было изменено. Повторите попытку",
                    String.format("Ошибка при удалении решения пользователем %s. Решение %s было " +
                            "изменено другим пользователем", principal.getId(), solution.getId())
            );
        }

        String pathStr = content.get(0).getPath();
        Path filePath = Paths.get(pathStr);
        Path dirPath = filePath.getParent();

        fileService.deleteDirectory(dirPath);
    }

    private SolutionStatus calculateSolutionStatus(Task task) {
        if (task.getToSubmitAt() == null) {
            return SolutionStatus.SUBMITTED;
        } else {
            return task.getToSubmitAt().isAfter(LocalDateTime.now()) ? SolutionStatus.SUBMITTED :
                    SolutionStatus.SUBMITTED_LATE;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewSolution(User principal, UUID solutionId, SolutionReviewRequestDto request) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Решения с id %s не существует",
                        String.format("Ошибка при оценке решения пользователем %s. " +
                                "Решения с id %s не существует", principal.getEmail(), solutionId)
                ));

//        if (solution.getStatus() == SolutionStatus.REVOKED) {
//            throw new AttemptToGetRevokedSolutionEx(
//                    "Ошибка доступа. У вас нет доступа к этому решению",
//                    String.format("Ошибка при оценке решения пользователем %s. Решение %s REVOKED",
//                            principal.getEmail(), solutionId)
//            );

        if (solution.getStatus() == SolutionStatus.ACCEPTED || solution.getStatus() == SolutionStatus.RETURNED) {
            throw new SolutionAlreadyReviewedEx(
                    "Ошибка. Решение уже было проверено",
                    String.format("Ошибка при проверке решения пользователем %s. Решение %s уже было проверено",
                            principal.getId(), solutionId)
            );
        }

        if (request.getStatus() == SolutionReviewStatus.RETURNED) {
            solution.setStatus(SolutionStatus.RETURNED);
        } else if (request.getStatus() == SolutionReviewStatus.ACCEPTED) {
            if (solution.getTask().isAssessed()) {
                if (request.getMark() == null) {
                    throw new SolutionReviewMarkMissingEx(
                            "Ошибка. Отсутствует балл",
                            String.format("Ошибка при проверке решения %s пользователем %s. Отсутствует балл",
                                    solutionId, principal.getId())
                    );
                }
                solution.setMark(request.getMark());
                solution.setStatus(SolutionStatus.ACCEPTED);
            }
        }

        solution.setReviewer(principal);
        solution.setReviewedAt(LocalDateTime.now());

        try {
            solutionRepository.saveAndFlush(solution);
        } catch (OptimisticLockException ex) {
            throw new EntityLockEx(
                    "Ошибка. Решение было изменено. Повторите попытку",
                    String.format("Ошибка при оценке решения пользователем %s. Решение %s было " +
                            "изменено другим пользователем", principal.getId(), solution.getId())
            );
        }
        solutionEmailService.sendSolutionReviewedNotification(solution.getUser(), solution);
    }

    public SolutionResponseDto findSolutionById(User principal, UUID solutionId) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого решения не существует",
                        String.format("Ошибка при поиске решения пользователем %s. " +
                                "Решения с id %s не существует", principal.getId(), solutionId)
                ));

//        if (solution.getStatus() == SolutionStatus.REVOKED && !principal.equals(solution.getUser())) {
//            throw new AttemptToGetRevokedSolutionEx(
//                    "Ошибка доступа. Это решение было отозвано пользователем",
//                    String.format("Ошибка при поиске решения пользователем %s. Решение %s было отозвано",
//                            principal.getEmail(), solutionId)
//            );
//        }

        return DtoFactory.makeSolutionResponseDto(solution);
    }

    public SolutionResponseDto findSolutionByTaskIdAndUserId(User principal, UUID taskId, UUID userId) {
        Solution solution = solutionRepository.findByUserIdAndTaskId(userId, taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Заданный пользователь не загрузил решение к этому заданию",
                        String.format("Ошибка при поиске решения пользователем %s по заданию %s и пользователю %s. " +
                                "Решения не существует", principal.getId(), taskId, userId)
                ));

//        if (solution.getStatus() == SolutionStatus.REVOKED && !principal.equals(solution.getUser())) {
//            throw new AttemptToGetRevokedSolutionEx(
//                    "Ошибка доступа. Это решение было отозвано пользователем",
//                    String.format("Ошибка при поиске решения пользователем %s. Решение %s было отозвано",
//                            principal.getEmail(), solution.getId())
//            );
//        }

        return DtoFactory.makeSolutionResponseDto(solution);
    }

    public Optional<Solution> findById(UUID solutionId) {
        return solutionRepository.findById(solutionId);
    }

    public List<SolutionWithUserShortResponseDto> findSolutionsByTaskId(
            User principal, UUID taskId, SortBy sortBy
    ) {
        Task task = taskService.findByIdWithSolutions(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого задания не существует",
                        String.format("Ошибка при поиске решений пользователем %s по заданию %s. " +
                                "Задания не существует", principal.getId(), taskId)
                ));

        Stream<SolutionWithUserShortResponseDto> resStream = task.getSolutions()
                .stream()
                // .filter(s -> s.getStatus() != SolutionStatus.REVOKED)
                .map(DtoFactory::makeSolutionWithUserShortResponseDto);

        if (sortBy == SortBy.USER) {
            resStream = resStream.sorted(Comparator.comparing(
                    dto -> dto.getUser().getSurname(),
                    String::compareTo
            ));
        } else if (sortBy == SortBy.SOLUTION_SUBMITTED_AT) {
            resStream = resStream.sorted(
                    Comparator.comparing(
                            SolutionWithUserShortResponseDto::getSubmittedAt,
                            LocalDateTime::compareTo
                    )
            );
        }

        return resStream.toList();
    }

    public List<SolutionWithUserShortResponseDto> findSolutionsByTaskId(
            User principal, UUID taskId, SortBy sortBy, boolean isNeedUnreviewed
    ) {
        Task task = taskService.findByIdWithSolutions(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого задания не существует",
                        String.format("Ошибка при поиске решений пользователем %s по заданию %s. " +
                                "Задания не существует", principal.getId(), taskId)
                ));

        Stream<SolutionWithUserShortResponseDto> resStream = task.getSolutions()
                .stream()
                .filter(s -> {
                    if (isNeedUnreviewed) {
                        return s.getStatus() == SolutionStatus.SUBMITTED ||
                                s.getStatus() == SolutionStatus.SUBMITTED_LATE;
                    } else {
                        return s.getStatus() == SolutionStatus.ACCEPTED ||
                                s.getStatus() == SolutionStatus.RETURNED;
                    }
                })
                .map(DtoFactory::makeSolutionWithUserShortResponseDto);

        if (sortBy == SortBy.USER) {
            resStream = resStream.sorted(Comparator.comparing(
                    dto -> dto.getUser().getSurname(),
                    String::compareTo
            ));
        } else if (sortBy == SortBy.SOLUTION_SUBMITTED_AT) {
            resStream = resStream.sorted(
                    Comparator.comparing(
                            SolutionWithUserShortResponseDto::getSubmittedAt,
                            LocalDateTime::compareTo
                    )
            );
        }

        return resStream.toList();
    }

    public List<SolutionWithTaskShortResponseDto> findSolutionsByCourseIdAndUserId(
            User principal, UUID courseId, UUID userId, SortBy sortBy
    ) {
        List<Solution> solutions = solutionRepository.findAllByCourseIdAndUserId(courseId, userId);

        Stream<SolutionWithTaskShortResponseDto> resStream = solutions.stream()
                // .filter(s -> s.getStatus() != SolutionStatus.REVOKED)
                .map(DtoFactory::makeSolutionWithTaskShortResponseDto);

        if (sortBy == SortBy.TASK_PUBLISHED_AT) {
            resStream = resStream.sorted(
                    Comparator.comparing(
                            dto -> dto.getTask().getPublishedAt(),
                            LocalDateTime::compareTo
                    )
            );
        } else if (sortBy == SortBy.SOLUTION_SUBMITTED_AT) {
            resStream = resStream.sorted(
                    Comparator.comparing(
                            SolutionWithTaskShortResponseDto::getSubmittedAt,
                            LocalDateTime::compareTo
                    )
            );
        }

        return resStream.toList();
    }

    public List<SolutionWithTaskShortResponseDto> findSolutionsByCourseIdAndUserId(
            User principal, UUID courseId, UUID userId, SortBy sortBy, boolean isNeedUnreviewed
    ) {
        List<Solution> solutions = solutionRepository.findAllByCourseIdAndUserId(courseId, userId);

        Stream<SolutionWithTaskShortResponseDto> resStream = solutions.stream()
                .filter(s -> {
                            if (isNeedUnreviewed) {
                                return s.getStatus() == SolutionStatus.SUBMITTED ||
                                        s.getStatus() == SolutionStatus.SUBMITTED_LATE;
                            } else {
                                return s.getStatus() == SolutionStatus.ACCEPTED ||
                                        s.getStatus() == SolutionStatus.RETURNED;
                            }
                        }
                )
                .map(DtoFactory::makeSolutionWithTaskShortResponseDto);

        if (sortBy == SortBy.TASK_PUBLISHED_AT) {
            resStream = resStream.sorted(
                    Comparator.comparing(
                            dto -> dto.getTask().getPublishedAt(),
                            LocalDateTime::compareTo
                    )
            );
        } else if (sortBy == SortBy.SOLUTION_SUBMITTED_AT) {
            resStream = resStream.sorted(
                    Comparator.comparing(
                            SolutionWithTaskShortResponseDto::getSubmittedAt,
                            LocalDateTime::compareTo
                    )
            );
        }

        return resStream.toList();
    }


    public List<SolutionsGroupByTaskShortResponseDto> findSolutionsByCourseIdGroupByTask(
            User principal, UUID courseId, SortBy sortBy) {
        List<Solution> solutions = solutionRepository.findAllByCourseId(courseId);

        Stream<SolutionsGroupByTaskShortResponseDto> resStream = solutions.stream()
                //.filter(s -> s.getStatus() != SolutionStatus.REVOKED)
                .sorted(Comparator.comparing(Solution::getSubmittedAt, LocalDateTime::compareTo))
                .collect(Collectors.groupingBy(Solution::getTask))
                .entrySet()
                .stream()
                .map(e -> DtoFactory.makeSolutionsGroupByTaskShortResponseDto(e.getKey(), e.getValue()));

        if (sortBy == SortBy.TASK_PUBLISHED_AT) {
            resStream = resStream.sorted(
                    Comparator.comparing(
                            dto -> dto.getTask().getPublishedAt(),
                            LocalDateTime::compareTo
                    )
            );
        }

        return resStream.toList();
    }

    // сортировка solutions по submittedAt
    public List<SolutionsGroupByTaskShortResponseDto> findSolutionsByCourseIdGroupByTask(
            User principal, UUID courseId, SortBy sortBy, boolean isNeedUnreviewed
    ) {
        List<Solution> solutions = solutionRepository.findAllByCourseId(courseId);

        Stream<SolutionsGroupByTaskShortResponseDto> resStream = solutions.stream()
                .filter(s -> {
                            if (isNeedUnreviewed) {
                                return s.getStatus() == SolutionStatus.SUBMITTED ||
                                        s.getStatus() == SolutionStatus.SUBMITTED_LATE;
                            } else {
                                return s.getStatus() == SolutionStatus.ACCEPTED ||
                                        s.getStatus() == SolutionStatus.RETURNED;
                            }
                        }
                )
                .sorted(Comparator.comparing(Solution::getSubmittedAt, LocalDateTime::compareTo))
                .collect(Collectors.groupingBy(Solution::getTask))
                .entrySet()
                .stream()
                .map(e -> DtoFactory.makeSolutionsGroupByTaskShortResponseDto(e.getKey(), e.getValue()));

        if (sortBy == SortBy.TASK_PUBLISHED_AT) {
            resStream = resStream.sorted(
                    Comparator.comparing(
                            dto -> dto.getTask().getPublishedAt(),
                            LocalDateTime::compareTo
                    )
            );
        }

        return resStream.toList();
    }

    public Resource getSolutionFile(String path) {
        Path absPath = Paths.get(basePath).resolve(path);

        try {
            Resource resource = new UrlResource(absPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Файл не найден");
            }

            return resource;
        } catch (MalformedURLException ex) {
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

//    public List<SolutionsGroupByCourseShortResponseDto> findSolutionsByUserIdGroupByCourse(
//            User principal, UUID userId, SortBy sortBy) {
//        List<Solution> solutions = solutionRepository.findAllByCourseId(courseId);
//
//        Stream<SolutionsGroupByTaskShortResponseDto> resStream = solutions.stream()
//                .filter(s -> {
//                            if (isNeedUnreviewed) {
//                                return s.getStatus() == SolutionStatus.SUBMITTED ||
//                                        s.getStatus() == SolutionStatus.SUBMITTED_LATE;
//                            } else {
//                                return s.getStatus() == SolutionStatus.ACCEPTED ||
//                                        s.getStatus() == SolutionStatus.RETURNED;
//                            }
//                        }
//                )
//                .sorted(Comparator.comparing(Solution::getSubmittedAt, LocalDateTime::compareTo))
//                .collect(Collectors.groupingBy(Solution::getTask))
//                .entrySet()
//                .stream()
//                .map(e -> DtoFactory.makeSolutionsGroupByTaskShortResponseDto(e.getKey(), e.getValue()));
//
//        if (sortBy == SortBy.TASK_PUBLISHED_AT) {
//            resStream = resStream.sorted(
//                    Comparator.comparing(
//                            dto -> dto.getTask().getPublishedAt(),
//                            LocalDateTime::compareTo
//                    )
//            );
//        }
//
//        return resStream.toList();
//    }
}
