package ru.antonov.oauth2_social.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.course.dto.*;
import ru.antonov.oauth2_social.course.entity.*;
import ru.antonov.oauth2_social.course.exception.CourseMaterialFileLimitExceededEx;
import ru.antonov.oauth2_social.course.exception.TaskAndMaterialFileLimitExceededEx;
import ru.antonov.oauth2_social.course.exception.UserAmountForCourseLimitExceededEx;
import ru.antonov.oauth2_social.course.repository.CourseMaterialRepository;
import ru.antonov.oauth2_social.course.repository.CourseRepository;
import ru.antonov.oauth2_social.course.repository.CourseUserRepository;
import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.exception.DBConstraintViolationEx;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.exception.UserAlreadyJoinedCourseEx;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseUserRepository courseUserRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final UserService userService;
    private final AccessManager accessManager;
    private final CourseEmailService courseEmailService;
    private final FileService fileService;
    private final CourseLimitCounter courseLimitCounter;

    @Value("${spring.application.course-limit-params.max-task-and-material-file-amount-for-course}")
    private int maxTaskAndMaterialFileAmountForCourse;
    @Value("${spring.application.course-limit-params.max-user-amount-for-course}")
    private int maxUserAmountForCourse;
    @Value("${spring.application.course-limit-params.max-solution-file-amount-for-course}")
    private int maxSolutionFileAmountForCourse;
    @Value("${spring.application.course-limit-params.max-file-amount-for-course-material}")
    private int maxFileAmountForCourseMaterial;
    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    public Course save(Course course) {
        return courseRepository.saveAndFlush(course);
    }

    public Optional<Course> findById(UUID courseId) {
        return courseRepository.findById(courseId);
    }

    public void changeCourse(UUID courseId, String newName) {
        Course course = findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Курса с id %s не существует", courseId),
                        String.format("Ошибка при изменении информации о курсе пользователем %s. " +
                                "Курса с id %s не существует", SecurityContextHolder.getContext().getAuthentication().getName(), courseId)
                ));

        course.setName(newName);

        try {
            courseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException ex) {
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique") &&
                    sqlEx.getMessage().toLowerCase().contains("name")) {
                message = "Ошибка. В данном учебном заведении уже существует курс с таким названием";
                debugMessage = String.format("Ошибка при изменении информации о курсе. " +
                        "В учебном заведении %s уже существует курс с названием %s", course.getInstitution().getId(), newName);
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseResponseDto save(CourseCreateWithUserIdListRequestDto request, User creator) {
        List<User> users = userService.findAllByIdList(request.getUserIdList());
        users.forEach(u ->
                checkUserHasAccessToOtherOrElseThrow(creator, u, false, true)
        );

        String code = generateCourseCode();
        Course course = EntityFactory.makeCourseEntity(request, creator.getInstitution(), code);

        return save(course, users, creator);
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseResponseDto save(CourseCreateWithGroupIdListRequestDto request, User creator) {
        List<User> users = userService.findAllByGroupIdList(request.getGroupIdList());
        users.forEach(u ->
                checkUserHasAccessToOtherOrElseThrow(creator, u, false, true)
        );

        String code = generateCourseCode();
        Course course = EntityFactory.makeCourseEntity(request, creator.getInstitution(), code);

        return save(course, users, creator);
    }

    private void checkUserHasAccessToOtherOrElseThrow(User user, User other, boolean isNeedToHaveHigherPriority,
                                                      boolean isNeedToBeInOneInstitute) {
        if (Objects.equals(user.getId(), other.getId())) {
            return;
        }

        if (!accessManager.isUserHasAccessToOther(user, other, isNeedToHaveHigherPriority, isNeedToBeInOneInstitute)) {
            throw new AccessDeniedEx(
                    String.format(
                            "Ошибка доступа. Вы не имеете доступа к пользователю %s %s %s",
                            other.getSurname(), other.getName(), other.getPatronymic()
                    ),
                    String.format("Ошибка доступа. Пользователю %s не разрешен доступ к пользователю %s", user.getEmail(), other.getEmail())
            );
        }
    }

    private CourseResponseDto save(Course course, List<User> users, User creator) {
        courseRepository.saveAndFlush(course);

        if (courseLimitCounter.isUserAmountForCourseExceedsLimit(users.size(), course.getId())) {
            throw new UserAmountForCourseLimitExceededEx(
                    String.format("Ошибка создания курса. Превышено максимально допустимое число пользователей" +
                            " в рамках курса - %s", maxUserAmountForCourse
                    ),
                    String.format("Ошибка создания курса. Ошибка создания курса пользователем %s. Превышено число " +
                            "maxUserAmountForCourse", creator.getId())

            );
        }

        Set<CourseUser> courseUsers = users.stream()
                .filter(u -> !u.equals(creator))
                .map(u -> {
                            CourseUserKey key = new CourseUserKey(course.getId(), u.getId());
                            return CourseUser
                                    .builder()
                                    .id(key)
                                    .user(u)
                                    .course(course)
                                    .build();

                        }
                )
                .collect(Collectors.toSet());

        course.setCourseUsers(courseUsers);

        CourseUserKey creatorKey = new CourseUserKey(course.getId(), creator.getId());
        course.addCourseUser(
                CourseUser
                        .builder()
                        .id(creatorKey)
                        .course(course)
                        .user(creator)
                        .isCreator(true)
                        .build()
        );

        try {
            courseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException ex) {
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique") &&
                    sqlEx.getMessage().toLowerCase().contains("name")) {
                message = "Ошибка. В данном учебном заведении уже существует курс с таким названием";
                debugMessage = String.format("Ошибка при создании курса. " +
                        "В учебном заведении %s уже существует курс с названием %s", course.getInstitution().getId(), course.getName());
            } else if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique") &&
                    sqlEx.getMessage().toLowerCase().contains("code")) {
                message = "Ошибка на сервере";
                debugMessage = String.format("Ошибка при создании курса. " +
                        "Курс с кодом %s уже существует", course.getCode());
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }

        final String courseName = course.getName();

        users.forEach(u -> courseEmailService.sendCourseJoinNotification(u, courseName));

        return DtoFactory.makeCourseResponseDto(
                course,
                creator,
                courseUsers.stream().map(CourseUser::getUser).toList()
        );
    }

    private void checkPrincipalHasAccessToCourseOrElseThrow(
            User principal, UUID courseId, boolean isNeedToBeCreator, boolean isNeedToHaveHigherRoleThanStudent) {
        if (!accessManager.isUserHasAccessToCourse(principal, courseId, isNeedToBeCreator, isNeedToHaveHigherRoleThanStudent)) {
            throw new AccessDeniedEx(
                    String.format("Ошибка доступа. У вас нет доступа к курсу %s.", courseId),
                    String.format("Отказ в доступе к курсу. Пользователь %s не имеет доступа к курсу %s",
                            principal.getEmail(), courseId)
            );
        }
    }

    private String generateCourseCode() {
        return CourseCodeGenerator.generateCourseCode();
    }

    public void deleteById(UUID courseId) {
        courseRepository.deleteById(courseId);
        String courseDirPath = basePath + "/" + courseId;
        fileService.deleteDirectory(courseDirPath);
    }

    public boolean isUserJoinedCourse(UUID userId, UUID courseId) {
        return courseUserRepository.existsById(
                CourseUserKey.builder().userId(userId).courseId(courseId).build()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public List<UserShortResponseDto> addUsersToCourseByUserIdList(UUID courseId, List<UUID> userIdList, User tutor) {
        Course course = findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Курса с указанным id не существует",
                        String.format("Ошибка при добавлении пользователей к курсу пользователем %s. " +
                                "Курса с id %s не существует", SecurityContextHolder.getContext().getAuthentication().getName(), courseId)
                ));

        List<User> users = userService.findAllByIdList(userIdList);
        return addUsersToCourse(course, users, tutor);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<UserShortResponseDto> addUsersToCourseByGroupIdList(UUID courseId, List<UUID> groupIdList, User tutor) {
        Course course = findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Курса с id %s не существует", courseId),
                        String.format("Ошибка при добавлении пользователей к курсу пользователем %s. " +
                                "Курса с id %s не существует", SecurityContextHolder.getContext().getAuthentication().getName(), courseId)
                ));

        List<User> users = userService.findAllByGroupIdList(groupIdList);
        return addUsersToCourse(course, users, tutor);
    }

    private void checkIsAnyUserAlreadyJoinedCourse(List<User> users, UUID courseId) {
        users.forEach(u -> {
            if (isUserJoinedCourse(u.getId(), courseId)) {
                throw new UserAlreadyJoinedCourseEx(
                        String.format("Ошибка. Пользователь %s %s %s уже является участником этого курса",
                                u.getSurname(), u.getName(), u.getPatronymic()),
                        String.format("Ошибка при добавлении пользователей к курсу. " +
                                "Пользователь %s уже является участником курса %s", u.getId(), courseId)
                );
            }
        });
    }

    private List<UserShortResponseDto> addUsersToCourse(Course course, List<User> users, User tutor) {
        users.forEach(u ->
                checkUserHasAccessToOtherOrElseThrow(tutor, u, false, true)
        );
        checkIsAnyUserAlreadyJoinedCourse(users, course.getId());

        if (courseLimitCounter.isUserAmountForCourseExceedsLimit(users.size(), course.getId())) {
            throw new UserAmountForCourseLimitExceededEx(
                    String.format("Ошибка добавления пользователей в курс. " +
                                    "Превышено максимально допустимое число пользователей в рамках курса - %s",
                            maxUserAmountForCourse
                    ),
                    String.format("Ошибка добавления пользователей в курс. Ошибка добавления пользователей в курс %s" +
                                    " пользователем %s. Превышено число maxUserAmountForCourse",
                            course.getId(), tutor.getId())
            );
        }

        Set<CourseUser> courseUsers = users.stream()
                .map(u -> {
                            CourseUserKey key = new CourseUserKey(course.getId(), u.getId());
                            return CourseUser
                                    .builder()
                                    .id(key)
                                    .course(course)
                                    .user(u)
                                    .build();
                        }
                )
                .collect(Collectors.toSet());

        course.addCourseUsers(courseUsers);

        courseRepository.saveAndFlush(course);

        users.forEach(u -> courseEmailService.sendCourseJoinNotification(u, course.getName()));

        return users
                .stream()
                .map(ru.antonov.oauth2_social.user.dto.DtoFactory::makeUserShortResponseDto)
                .toList();
    }

    public void removeUsersFromCourseByUserIdList(UUID courseId, List<UUID> userIdList) {
        Course course = findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Курса с id %s не существует", courseId),
                        String.format("Ошибка при добавлении пользователей к курсу пользователем %s. " +
                                "Курса с id %s не существует", SecurityContextHolder.getContext().getAuthentication().getName(), courseId)
                ));
        List<User> users = userService.findAllByIdList(userIdList);

        course.removeUsers(users);

        courseRepository.saveAndFlush(course);
    }

    public CourseResponseDto getCourseInfoById(User principal, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Курса с id %s не существует", courseId),
                        String.format("Ошибка при получении информации о курсе. Курса с id %s не существует", courseId)
                ));

        List<CourseUser> courseUsers = courseUserRepository.findAllByCourseId(courseId);
        List<User> users = courseUsers.stream()
                .map(CourseUser::getUser)
                .toList();

        User creator = courseUsers.stream()
                .filter(CourseUser::isCreator)
                .map(CourseUser::getUser)
                .findFirst()
                .orElse(null);


        CourseResponseDto courseResponse = DtoFactory.makeCourseResponseDto(course, creator, users);

        if (!principal.getRole().equals(Role.ADMIN) && !principal.getRole().equals(Role.TUTOR)) {
            courseResponse.setCode(null);
        }

        return courseResponse;
    }

    public List<CourseResponseDto> findCoursesByInstitutionId(User principal, UUID institutionId) {
        List<Course> courses = courseRepository.findAllByInstitutionId(institutionId);

        return courses.stream()
                .map(c -> {

                    CourseResponseDto courseResponse = DtoFactory.makeCourseResponseDto(
                            c,
                            courseUserRepository.findCourseCreatorByCourseId(c.getId()).orElse(null),
                            courseUserRepository.findAllByCourseId(c.getId())
                                    .stream()
                                    .map(CourseUser::getUser)
                                    .toList()
                    );
                    if (!principal.getRole().equals(Role.ADMIN) && !principal.getRole().equals(Role.TUTOR)) {
                        courseResponse.setCode(null);
                    }
                    return courseResponse;

                })
                .toList();
    }

    public List<CourseResponseDto> findCoursesByUser(User principal) {
        List<Course> courses = principal.getRole().equals(Role.ADMIN) ?
                courseRepository.findAllByInstitutionId(principal.getInstitution().getId()) :
                courseUserRepository.findCoursesByUserId(principal.getId());

        return courses.stream()
                .map(c -> {

                    CourseResponseDto courseResponse = DtoFactory.makeCourseResponseDto(
                            c,
                            courseUserRepository.findCourseCreatorByCourseId(c.getId()).orElse(null),
                            courseUserRepository.findAllByCourseId(c.getId())
                                    .stream()
                                    .map(CourseUser::getUser)
                                    .toList()
                    );
                    if (!principal.getRole().equals(Role.ADMIN) && !principal.getRole().equals(Role.TUTOR)) {
                        courseResponse.setCode(null);
                    }
                    return courseResponse;

                })
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseMaterialResponseDto saveCourseMaterial(User principal, UUID courseId, CourseMaterialCreateRequestDto request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Данного курса не существует",
                        String.format("Ошибка при загрузке файлов в курс. Курса с id %s не существует", courseId)
                ));

        UUID courseMaterialId = UUID.randomUUID();

        List<MultipartFile> files = request.getContent();

        if (courseLimitCounter.isTaskAndMaterialFileAmountExceedsLimit(course.getId(), files.size())) {
            throw new TaskAndMaterialFileLimitExceededEx(
                    "Ошибка при загрузке файлов. Превышено максимально допустимое число файлов для этого курса",
                    String.format("Ошибка при загрузке файлов. " +
                            "Превышено максимально допустимое количество учебных файлов для курса с %s id", course.getId())
            );
        }

        List<FileService.FileInfo> contentInfoList = files.stream()
                .map(f -> {
                    UUID fileId = UUID.randomUUID();
                    String path = fileService.generateCourseMaterialContentFilePath(
                            course.getId(), courseMaterialId, fileId, getFileExtension(f.getOriginalFilename())
                    );
                    return new FileService.FileInfo(path, f.getOriginalFilename(), f, fileId);
                })
                .toList();

        List<Content> content = contentInfoList.stream()
                .map(ci -> Content
                        .builder()
                        .id(ci.getId())
                        .originalFileName(ci.getFileName())
                        .path(ci.getPath())
                        .build()
                )
                .toList();

        CourseMaterial courseMaterial = CourseMaterial
                .builder()
                .id(courseMaterialId)
                .topic(request.getTopic())
                .content(content)
                .publishedAt(LocalDateTime.now())
                .course(course)
                .user(principal)
                .build();

        course.addCourseMaterial(courseMaterial);

        try {
            courseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException ex) {
            Throwable root = NestedExceptionUtils.getRootCause(ex);
            String message;
            String debugMessage;

            if (root instanceof SQLException sqlEx && sqlEx.getMessage().toLowerCase().contains("unique") &&
                    sqlEx.getMessage().toLowerCase().contains("course_id_topic")) {
                message = "Ошибка. В данном курсе уже существует учебный материал с таким названием";
                debugMessage = String.format("Ошибка при добавлении учебного материала. " +
                        "В курсе %s уже существует учебный материал с названием %s", course.getId(), request.getTopic());
            } else {
                message = "Нарушены ограничения целостности данных. Возможно, вы пытаетесь добавить некорректные или уже существующие данные";
                debugMessage = (root != null ? root.getMessage() : ex.getMessage());
            }

            throw new DBConstraintViolationEx(message, debugMessage);
        }

        fileService.uploadCourseMaterialContentFiles(contentInfoList);

        return DtoFactory.makeCourseMaterialResponseDto(courseMaterial);
    }

    public Optional<CourseMaterial> findCourseMaterialById(UUID id) {
        return courseMaterialRepository.findById(id);
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Ошибка получения расширения файла. filename = null");
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new IllegalArgumentException("Ошибка получения расширения файла. filename не содержит расширение");
        }

        return filename.substring(dotIndex + 1);
    }

    public void deleteCourseMaterial(CourseMaterial courseMaterial) {
        // удалить из базы
        // стереть файлы

        List<Content> content = courseMaterial.getContent();
        courseMaterialRepository.delete(courseMaterial);

        // base/courses/id/course_materials/id/filename
        String path = content.get(0).getPath();
        int slashLastIndex = path.lastIndexOf('/');
        String dirPath = path.substring(0, slashLastIndex);

        fileService.deleteDirectory(dirPath);
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseMaterialResponseDto updateCourseMaterial(
            User principal, CourseMaterial courseMaterial, CourseMaterialUpdateRequestDto request
    ) {

        String newTopic = request.getNewTopic();
        if (newTopic != null && !newTopic.isBlank()) {
            courseMaterial.setTopic(newTopic);
        }

        // удалить файлы
        List<Content> content = courseMaterial.getContent();
        List<UUID> toDeleteList = request.getToDeleteList();

        content = content.stream()
                .filter(c -> {
                    boolean isToDeleteAndSkip = toDeleteList.contains(c.getId());
                    if (isToDeleteAndSkip) {
                        fileService.deleteFile(c.getPath());
                    }
                    return !isToDeleteAndSkip;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        courseMaterial.setContent(content);

        List<MultipartFile> newContent = request.getContent();

        if (courseLimitCounter.isTaskAndMaterialFileAmountExceedsLimit(courseMaterial.getCourse().getId(), newContent.size())) {
            throw new TaskAndMaterialFileLimitExceededEx(
                    String.format("Ошибка при загрузке файлов. Превышено максимально " +
                                    "допустимое число файлов для этого курса - %s",
                            maxUserAmountForCourse
                    ),
                    String.format("Ошибка при загрузке файлов. " +
                                    "Превышено максимально допустимое количество учебных файлов для курса с %s id",
                            courseMaterial.getCourse().getId()
                    )
            );
        } else if (courseLimitCounter.isFileAmountForCourseMaterialExceedsLimit(
                courseMaterial.getCourse().getId(), courseMaterial.getId(), newContent.size()
        )) {
            throw new CourseMaterialFileLimitExceededEx(
                    String.format("Ошибка при загрузке файлов. Превышено максимально допустимое число файлов" +
                            " в рамках одного учебного материала - %d", maxFileAmountForCourseMaterial),
                    String.format("Ошибка при загрузке файлов пользователем %s в курс %s. " +
                                    "Превышено максимально допустимое число файлов в рамках одного учебного материала",
                            principal.getId(), courseMaterial.getCourse().getId())
            );
        }

        List<FileService.FileInfo> contentInfoList = newContent.stream()
                .map(f -> {
                    UUID fileId = UUID.randomUUID();
                    String path = fileService.generateCourseMaterialContentFilePath(
                            courseMaterial.getCourse().getId(), courseMaterial.getId(), fileId,
                            getFileExtension(f.getOriginalFilename())
                    );
                    return new FileService.FileInfo(path, f.getOriginalFilename(), f, fileId);
                })
                .toList();

        List<Content> courseMaterialContent = contentInfoList.stream()
                .map(ci -> Content
                        .builder()
                        .id(ci.getId())
                        .originalFileName(ci.getFileName())
                        .path(ci.getPath())
                        .build()
                )
                .collect(Collectors.toCollection(ArrayList::new));

        courseMaterial.addCourseMaterialContent(courseMaterialContent);
        courseMaterial.setLastChangedAt(LocalDateTime.now());
        courseMaterialRepository.saveAndFlush(courseMaterial);

        fileService.uploadCourseMaterialContentFiles(contentInfoList);

        return DtoFactory.makeCourseMaterialResponseDto(courseMaterial);
    }

    public int getUserCountForCourseByCourseId(UUID courseId) {
        return courseUserRepository.getUserCountForCourseByCourseId(courseId);
    }

    @Transactional
    public List<CourseMaterialResponseDto> findAllCourseMaterialsByCourseId(User principal, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Данного курса не существует",
                        String.format("Ошибка при получении учебных материалов. Курса с id %s не существует", courseId)
                ));

        return course.getCourseMaterials()
                .stream()
                .map(DtoFactory::makeCourseMaterialResponseDto)
                .toList();
    }

    public Resource getCourseMaterialFile(String path){
        Path filePath = Paths.get(path);

        try {
            Resource resource = new UrlResource(filePath.toUri());
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

}

