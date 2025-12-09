package ru.antonov.oauth2_social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.antonov.oauth2_social.course.entity.*;
import ru.antonov.oauth2_social.common.exception.AppConfigurationEx;
import ru.antonov.oauth2_social.course.repository.*;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.solution.entity.SolutionComment;
import ru.antonov.oauth2_social.solution.repository.SolutionCommentRepository;
import ru.antonov.oauth2_social.solution.repository.SolutionRepository;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.task.entity.TaskComment;
import ru.antonov.oauth2_social.task.entity.TaskUserKey;
import ru.antonov.oauth2_social.task.repository.TaskCommentRepository;
import ru.antonov.oauth2_social.task.repository.TaskRepository;
import ru.antonov.oauth2_social.task.repository.TaskUserRepository;
import ru.antonov.oauth2_social.user.entity.Group;
import ru.antonov.oauth2_social.user.entity.Role;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.user.repository.GroupRepository;

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

    private final TaskCommentRepository taskCommentRepository;
    private final SolutionCommentRepository solutionCommentRepository;

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

    public boolean isPrincipalHasAccessToEditTaskComment(User principal, UUID commentId) {
        Optional<TaskComment> commentOpt = taskCommentRepository.findById(commentId);
        if (commentOpt.isEmpty()) return false;

        TaskComment comment = commentOpt.get();

        User commentAuthor = comment.getAuthor();

        boolean isCommentAuthor = commentAuthor != null && principal.getId().equals(commentAuthor.getId());
        boolean hasAccessToTask = isPrincipalHasAccessToTask(principal, comment.getTask().getId());
        boolean hasMorePrivilegedRoleThanCommentAuthor = commentAuthor != null &&
                principal.getRole().getPriorityValue() > commentAuthor.getRole().getPriorityValue();

        return isCommentAuthor || (hasAccessToTask && hasMorePrivilegedRoleThanCommentAuthor);
    }

    public boolean isPrincipalHasAccessToEditSolutionComment(User principal, UUID commentId) {
        Optional<SolutionComment> commentOpt = solutionCommentRepository.findById(commentId);
        if (commentOpt.isEmpty()) return false;

        SolutionComment comment = commentOpt.get();

        User commentAuthor = comment.getAuthor();

        boolean isCommentAuthor = commentAuthor != null && principal.getId().equals(commentAuthor.getId());
        boolean hasAccessToSolution = isPrincipalHasAccessToSolution(principal, comment.getSolution().getId());
        boolean hasMorePrivilegedRoleThanCommentAuthor = commentAuthor != null &&
                principal.getRole().getPriorityValue() > commentAuthor.getRole().getPriorityValue();

        return isCommentAuthor || (hasAccessToSolution && hasMorePrivilegedRoleThanCommentAuthor);
    }
}
