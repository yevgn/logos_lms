package ru.antonov.oauth2_social.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.antonov.oauth2_social.config.AccessManager;
import ru.antonov.oauth2_social.course.dto.*;
import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.entity.CourseUser;
import ru.antonov.oauth2_social.course.entity.CourseUserKey;
import ru.antonov.oauth2_social.course.entity.EntityFactory;
import ru.antonov.oauth2_social.course.repository.CourseRepository;
import ru.antonov.oauth2_social.course.repository.CourseUserRepository;
import ru.antonov.oauth2_social.exception.AccessDeniedEx;
import ru.antonov.oauth2_social.exception.DBConstraintViolationEx;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.exception.UserAlreadyJoinedCourseEx;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.service.UserService;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseUserRepository courseUserRepository;
    private final UserService userService;
    private final AccessManager accessManager;
    private final EmailService emailService;

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
    public CourseCreateResponseDto save(CourseCreateWithUserIdListRequestDto request, User creator) {
        List<User> users = userService.findAllByIdList(request.getUserIdList());
        users.forEach(u ->
                checkUserHasAccessToOtherOrElseThrow(creator, u, false, true)
        );

        String code = generateCourseCode();
        Course course = EntityFactory.makeCourseEntity(request, creator.getInstitution(), code);

        return save(course, users, creator);
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseCreateResponseDto save(CourseCreateWithGroupIdListRequestDto request, User creator) {
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

    private CourseCreateResponseDto save(Course course, List<User> users, User creator) {
        Set<CourseUser> courseUsers = users.stream()
                .map(u -> CourseUser
                        .builder()
                        .user(u)
                        .build()
                )
                .collect(Collectors.toSet());
        courseUsers.add(
                CourseUser
                        .builder()
                        .course(course)
                        .user(creator)
                        .isCreator(true)
                        .build()
        );

        course.setCourseUsers(courseUsers);

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

        users.forEach(u -> emailService.sendCourseJoinNotification(u, courseName));

        return DtoFactory.makeCourseCreateResponseDto(course, creator, users);
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
        // todo удалять файлы
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
                        String.format("Ошибка. Курса с id %s не существует", courseId),
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

        Set<CourseUser> courseUsers = users.stream()
                .map(u -> CourseUser
                        .builder()
                        .course(course)
                        .user(u)
                        .build()
                )
                .collect(Collectors.toSet());

        course.addCourseUsers(courseUsers);

        courseRepository.saveAndFlush(course);

        users.forEach(u -> emailService.sendCourseJoinNotification(u, course.getName()));

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
}

