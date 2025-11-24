package ru.antonov.oauth2_social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.antonov.oauth2_social.course.entity.*;
import ru.antonov.oauth2_social.exception.AppConfigurationEx;
import ru.antonov.oauth2_social.course.repository.*;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.solution.repository.SolutionRepository;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.task.entity.TaskUserKey;
import ru.antonov.oauth2_social.task.repository.TaskRepository;
import ru.antonov.oauth2_social.task.repository.TaskUserRepository;
import ru.antonov.oauth2_social.user.entity.Group;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.repository.GroupRepository;
import ru.antonov.oauth2_social.user.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessManager {
    private final CourseUserRepository courseUserRepository;
    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final SolutionRepository solutionRepository;
    private final TaskUserRepository taskUserRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public boolean isUserHasAccessToOther(User userRequestedForAccess, User toAccessTo, boolean isNeedToHaveHigherPriority,
                                          boolean isNeedToBeInOneInstitution) {

        boolean isHasAccess;

        if (isNeedToHaveHigherPriority) {
            isHasAccess = userRequestedForAccess.getRole().getPriorityValue() > toAccessTo.getRole().getPriorityValue();
        } else {
            isHasAccess = userRequestedForAccess.getRole().getPriorityValue() >= toAccessTo.getRole().getPriorityValue();
        }

        if (isNeedToBeInOneInstitution) {
            Objects.requireNonNull(
                    userRequestedForAccess.getInstitution(),
                    "Ошибка при проверке isUserHasAccessToOther() в AccessManager. institution у userRequestedForAccess must not be null"
            );
            isHasAccess = isHasAccess && Objects.equals(userRequestedForAccess.getInstitution().getId(), toAccessTo.getInstitution().getId());
        }

        return isHasAccess;
    }

    public boolean isPrincipalHasAccessToTask(User user, UUID taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return false;
        }

        Task task = taskOpt.get();

        if (user.getRole().equals(Role.STUDENT)) {
            // если задание - общее, проверить есть ли он в курсе
            if (task.isForEveryone()) {
                return courseUserRepository.findById(
                        CourseUserKey.builder().courseId(task.getCourse().getId()).userId(user.getId()).build()
                ).isPresent();
            } else {
                return taskUserRepository.findById(
                        TaskUserKey.builder().taskId(taskId).userId(user.getId()).build()
                ).isPresent();
            }
        } else if (user.getRole().equals(Role.ADMIN)) {
            return task.getCourse().getInstitution().equals(user.getInstitution());
        } else if (user.getRole().equals(Role.TUTOR)) {
            return courseUserRepository.findById(
                    CourseUserKey.builder().courseId(task.getCourse().getId()).userId(user.getId()).build()
            ).isPresent();
        } else {
            throw new AppConfigurationEx(
                    "Ошибка на сервере",
                    String.format("Ошибка проверки доступа пользователя %s к заданию %s. Не уставновлены правила доступа" +
                            " для роли %s", user.getEmail(), taskId, user.getRole().name())
            );
        }
    }

    public boolean isPrincipalHasAccessToSolution(User user, UUID solutionId) {
        Optional<Solution> solutionOpt = solutionRepository.findById(solutionId);
        if (solutionOpt.isEmpty()) {
            return false;
        }

        Solution solution = solutionOpt.get();

        if (user.getRole().equals(Role.STUDENT)) {
            return solution.getUser().equals(user);
        } else if (user.getRole().equals(Role.ADMIN)) {
            return solution.getTask().getCourse().getInstitution().equals(user.getInstitution());
        } else if (user.getRole().equals(Role.TUTOR)) {
            return courseUserRepository.existsById(
                    CourseUserKey.builder().userId(user.getId()).courseId(solution.getTask().getCourse().getId()).build()
            );
        } else {
            throw new AppConfigurationEx(
                    "Ошибка на сервере",
                    String.format("Ошибка проверки доступа пользователя %s к решению %s. Не уставновлены правила доступа" +
                            " для роли %s", user.getEmail(), solutionId, user.getRole().name())
            );
        }
    }

    public boolean isUserHasAccessToInstitution(User userRequestedForAccess, UUID instituteId) {
        Objects.requireNonNull(
                userRequestedForAccess.getInstitution(),
                "Ошибка при проверке isUserHasAccessToInstitution() в AccessManager. institution у userRequestedForAccess must not be null"
        );

        return Objects.equals(userRequestedForAccess.getInstitution().getId(), instituteId);
    }

    public boolean isUserHasAccessToGroup(User userRequestedForAccess, UUID groupId) {
        Objects.requireNonNull(
                userRequestedForAccess.getInstitution(),
                "Ошибка при проверке isUserHasAccessToGroup() в AccessManager. group у userRequestedForAccess must not be null"
        );

        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if(groupOpt.isEmpty()) return false;

        if(userRequestedForAccess.getRole() == Role.ADMIN || userRequestedForAccess.getRole() == Role.TUTOR) {
            return groupOpt.get().getInstitution().getId().equals(userRequestedForAccess.getInstitution().getId());
        } else if(userRequestedForAccess.getRole() == Role.STUDENT){
            return userRequestedForAccess.getGroup().getId().equals(groupId);
        } else {
            throw new AppConfigurationEx(
                    "Ошибка на сервере",
                    String.format("Ошибка проверки доступа пользователя %s к группе %s. Не уставновлены правила доступа" +
                            " для роли %s", userRequestedForAccess.getId(), groupId, userRequestedForAccess.getRole().name())
            );
        }
    }

    public boolean isUserHasAccessToCourse(
            User user, UUID courseId, boolean isNeedToBeCreator, boolean isNeedToHaveHigherRoleThanStudent) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }

        Course course = courseOpt.get();

        if (user.getRole().equals(Role.ADMIN)) {
            Objects.requireNonNull(
                    user.getInstitution(),
                    "Ошибка при проверке isUserHasAccessToCourse() в AccessManager. institution у user must not be null"
            );
            return Objects.equals(user.getInstitution(), course.getInstitution());
        }

        Optional<CourseUser> courseUserOpt = courseUserRepository.findById(
                CourseUserKey
                        .builder()
                        .userId(user.getId())
                        .courseId(courseId)
                        .build()
        );

        // если юзер - админ, то только проверяем равенство его института институту курса
        // если не админ, то проверяем наличие courseUser

        if (courseUserOpt.isEmpty()) {
            return false;
        }

        CourseUser courseUser = courseUserOpt.get();

        if (isNeedToBeCreator) {
            return courseUser.isCreator();
        }

        if (isNeedToHaveHigherRoleThanStudent) {
            return user.getRole().getPriorityValue() > Role.STUDENT.getPriorityValue();
        }

        return true;
    }

    public boolean isPrincipalHasAccessToTaskComment(User principal, UUID taskId, UUID commentId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;

        Task task = taskOpt.get();

        Optional<Task.TaskComment> commentOpt = task.getComments()
                .stream()
                .filter(t -> t.getId().equals(commentId))
                .findFirst();

        if (commentOpt.isEmpty()) return false;

        Task.TaskComment comment = commentOpt.get();

        Optional<User> commentAuthor = userRepository.findById(comment.getUserId());

        if (principal.getRole().equals(Role.ADMIN)) {
            return isPrincipalHasAccessToTask(principal, taskId);
        } else if (principal.getRole().equals(Role.TUTOR) || principal.getRole().equals(Role.STUDENT)) {
            return isPrincipalHasAccessToTask(principal, taskId) &&
                    principal.getRole().getPriorityValue() >
                            commentAuthor.map(user -> user.getRole().getPriorityValue()).orElse(0);
        } else {
            throw new AppConfigurationEx(
                    "Ошибка на сервере",
                    String.format("Ошибка проверки доступа пользователя %s к комментарию %s. Не уставновлены правила доступа" +
                            " для роли %s", principal.getEmail(), commentId, principal.getRole().name())
            );
        }
    }

    public boolean isPrincipalHasAccessToSolutionComment(User principal, UUID solutionId, UUID commentId) {
        Optional<Solution> solutionOpt = solutionRepository.findById(solutionId);
        if (solutionOpt.isEmpty()) return false;

        Solution solution = solutionOpt.get();

        Optional<Solution.SolutionComment> commentOpt = solution.getComments()
                .stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst();

        if (commentOpt.isEmpty()) return false;

        Solution.SolutionComment comment = commentOpt.get();

        Optional<User> commentAuthor = userRepository.findById(comment.getUserId());

        return isPrincipalHasAccessToSolution(principal, solutionId) &&
                (principal.getRole().getPriorityValue() >
                        commentAuthor.map(user -> user.getRole().getPriorityValue()).orElse(0))
                || principal.getId().equals(comment.getUserId());
    }
}
