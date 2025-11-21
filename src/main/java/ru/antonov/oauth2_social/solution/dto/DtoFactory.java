package ru.antonov.oauth2_social.solution.dto;

import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;

public class DtoFactory {
    public static SolutionResponseDto makeSolutionResponseDto(Solution solution){
        return SolutionResponseDto
                .builder()
                .id(solution.getId())
                .solutionStatus(solution.getStatus())
                .content(
                        solution.getContent()
                                .stream()
                                .map(ru.antonov.oauth2_social.course.dto.DtoFactory::makeContentFileResponseDto)
                                .toList()
                )
                .submittedAt(solution.getSubmittedAt())
                .mark(solution.getMark())
                .reviewedBy( solution.getReviewer() == null ? null :
                        ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(solution.getReviewer()))
                .reviewedAt(solution.getReviewedAt())
                .user(ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(solution.getUser()))
                .build();
    }

    public static SolutionShortResponseDto makeSolutionShortResponseDto(Solution solution){
        return SolutionShortResponseDto
                .builder()
                .id(solution.getId())
                .status(solution.getStatus())
                .submittedAt(solution.getSubmittedAt())
                .mark(solution.getMark())
                .reviewedAt(solution.getReviewedAt())
                .build();
    }

    public static SolutionWithTaskShortResponseDto makeSolutionWithTaskShortResponseDto(Solution solution){
        return SolutionWithTaskShortResponseDto
                .builder()
                .id(solution.getId())
                .status(solution.getStatus())
                .submittedAt(solution.getSubmittedAt())
                .mark(solution.getMark())
                .reviewedAt(solution.getReviewedAt())
                .task(ru.antonov.oauth2_social.task.dto.DtoFactory.makeTaskShortResponseDto(solution.getTask()))
                .build();
    }


    public static SolutionWithUserShortResponseDto makeSolutionWithUserShortResponseDto(Solution solution){
        return SolutionWithUserShortResponseDto
                .builder()
                .id(solution.getId())
                .status(solution.getStatus())
                .submittedAt(solution.getSubmittedAt())
                .reviewedAt(solution.getReviewedAt())
                .mark(solution.getMark())
                .user(ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(solution.getUser()))
                .build();
    }

    public static SolutionsGroupByTaskShortResponseDto makeSolutionsGroupByTaskShortResponseDto(
            Task task, List<Solution> solutions
    ){
        return SolutionsGroupByTaskShortResponseDto.builder()
                .task(ru.antonov.oauth2_social.task.dto.DtoFactory.makeTaskShortResponseDto(task))
                .solutions(
                        solutions.stream()
                                .map(DtoFactory::makeSolutionWithUserShortResponseDto)
                                .toList()
                )
                .build();
    }

    public static SolutionCommentResponseDto makeSolutionCommentResponseDto(
            Solution.SolutionComment comment, User author
    ){
        return SolutionCommentResponseDto
                .builder()
                .id(comment.getId())
                .text(comment.getText())
                .publishedAt(comment.getPublishedAt())
                .author(author == null ? null : ru.antonov.oauth2_social.user.dto.DtoFactory.makeUserShortResponseDto(author))
                .build();
    }
}
