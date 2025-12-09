package ru.antonov.oauth2_social.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.antonov.oauth2_social.course.service.CourseLimitCounter;

import ru.antonov.oauth2_social.common.exception.EntityNotFoundEx;

import ru.antonov.oauth2_social.task.dto.DtoFactory;
import ru.antonov.oauth2_social.task.dto.TaskCommentCreateRequestDto;
import ru.antonov.oauth2_social.task.dto.TaskCommentResponseDto;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.task.entity.TaskComment;
import ru.antonov.oauth2_social.task.exception.TaskCommentAmountLimitExceededEx;
import ru.antonov.oauth2_social.task.repository.TaskCommentRepository;
import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskCommentService {
    private final TaskCommentRepository taskCommentRepository;
    private final TaskService taskService;

    private final CourseLimitCounter courseLimitCounter;

    @Value("${spring.application.course-limit-params.max-comment-amount-for-task}")
    private int maxCommentAmountForTask;

    public TaskCommentResponseDto saveComment(User principal, UUID taskId, TaskCommentCreateRequestDto request) {
        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого задания не существует",
                        String.format("Ошибка при загрузке комментария к заданию пользователем %s. " +
                                "Задания %s не существует", principal.getId(), taskId)
                ));

        if (courseLimitCounter.isCommentAmountForTaskExceedsLimit(taskId, 1)) {
            throw new TaskCommentAmountLimitExceededEx(
                    String.format("Ошибка. Превышено максимальное число комментариев для задания - %s",
                            maxCommentAmountForTask),
                    String.format("Ошибка добавления комментария к заданию %s пользователем %s. Превышен лимит",
                            taskId, principal.getId())
            );
        }

        TaskComment comment = TaskComment.builder()
                .id(UUID.randomUUID())
                .author(principal)
                .task(task)
                .text(request.getText())
                .publishedAt(LocalDateTime.now())
                .build();

        taskCommentRepository.save(comment);

        return DtoFactory.makeTaskCommentResponseDto(comment);
    }

    public List<TaskCommentResponseDto> findAllByTaskIdSortByPublishedAt(User principal, UUID taskId){
        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого задания не существует",
                        String.format("Ошибка при поиске комментариев к заданию пользователем %s. " +
                                "Задания %s не существует", principal.getId(), taskId)
                ));

        return taskCommentRepository.findAllByTaskIdSortByPublishedAt(taskId)
                .stream()
                .map(DtoFactory::makeTaskCommentResponseDto)
                .toList();
    }

    public void deleteCommentById(User principal, UUID commentId) {
        taskCommentRepository.deleteById(commentId);
    }
}
