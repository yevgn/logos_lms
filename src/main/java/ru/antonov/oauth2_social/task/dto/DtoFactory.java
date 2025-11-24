package ru.antonov.oauth2_social.task.dto;

import ru.antonov.oauth2_social.solution.dto.SolutionCommentResponseDto;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;

public class DtoFactory {
    public static TaskResponseDto makeTaskResponseDto(Task task, List<User> targetUserList){
        return TaskResponseDto
                .builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .isAssessed(task.isAssessed())
                .publishedAt(task.getPublishedAt())
                .lastChangedAt(task.getLastChangedAt())
                .toSubmitAt(task.getToSubmitAt())
                .author(
                        task.getCreator() != null ?
                                ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(task.getCreator()) :
                                null
                )
                .content(
                        task.getContent()
                                .stream()
                                .map(ru.antonov.oauth2_social.course.dto.DtoFactory::makeContentFileResponseDto)
                                .toList()
                )
                .isForEveryone(task.isForEveryone())
                .targetUserList(
                        targetUserList
                                .stream()
                                .map(ru.antonov.oauth2_social.user.dto.DtoFactory::makeUserShortResponseDto)
                                .toList()
                )
                .build();
    }

    public static TaskShortResponseDto makeTaskShortResponseDto(Task task){
        return TaskShortResponseDto
                .builder()
                .id(task.getId())
                .title(task.getTitle())
                .isAssessed(task.isAssessed())
                .publishedAt(task.getPublishedAt())
                .lastChangedAt(task.getLastChangedAt())
                .toSubmitAt(task.getToSubmitAt())
                .author(
                        task.getCreator() != null ?
                                ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(task.getCreator()) :
                                null
                )
                .isForEveryone(task.isForEveryone())
                .build();
    }

    public static TaskCommentResponseDto makeTaskCommentResponseDto(
            Task.TaskComment comment, User author
    ){
        return TaskCommentResponseDto
                .builder()
                .id(comment.getId())
                .text(comment.getText())
                .publishedAt(comment.getPublishedAt())
                .author(author == null ? null : ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(author))
                .build();
    }
}
