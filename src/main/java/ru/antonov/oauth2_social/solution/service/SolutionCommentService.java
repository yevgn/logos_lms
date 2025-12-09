package ru.antonov.oauth2_social.solution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.antonov.oauth2_social.course.service.CourseLimitCounter;
import ru.antonov.oauth2_social.common.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.solution.dto.DtoFactory;
import ru.antonov.oauth2_social.solution.dto.SolutionCommentCreateRequestDto;
import ru.antonov.oauth2_social.solution.dto.SolutionCommentResponseDto;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.solution.entity.SolutionComment;

import ru.antonov.oauth2_social.solution.repository.SolutionCommentRepository;
import ru.antonov.oauth2_social.task.exception.TaskCommentAmountLimitExceededEx;
import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionCommentService {
    private final SolutionCommentRepository solutionCommentRepository;
    private final SolutionService solutionService;

    private final CourseLimitCounter courseLimitCounter;

    @Value("${spring.application.course-limit-params.max-comment-amount-for-solution}")
    private int maxCommentAmountForSolution;

    public SolutionCommentResponseDto saveComment(User principal, UUID solutionId, SolutionCommentCreateRequestDto request) {
        Solution solution = solutionService.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого решения не существует",
                        String.format("Ошибка при загрузке комментария к решению пользователем %s. " +
                                "Решения %s не существует", principal.getId(), solutionId)
                ));

//        if (solution.getStatus() == SolutionStatus.REVOKED) {
//            throw new AttemptToGetRevokedSolutionEx(
//                    "Ошибка доступа. У вас нет доступа к этому решению",
//                    String.format("Ошибка при добавлении решения пользователем %s. Решение %s REVOKED",
//                            principal.getId(), solutionId)
//            );
//        }

        if (courseLimitCounter.isCommentAmountForSolutionExceedsLimit(solutionId, 1)) {
            throw new TaskCommentAmountLimitExceededEx(
                    String.format("Ошибка. Превышено максимальное число комментариев для решения - %s",
                            maxCommentAmountForSolution),
                    String.format("Ошибка добавления комментария к решению %s пользователем %s. Превышен лимит",
                            solutionId, principal.getId())
            );
        }

        SolutionComment comment = SolutionComment.builder()
                .id(UUID.randomUUID())
                .author(principal)
                .solution(solution)
                .text(request.getText())
                .publishedAt(LocalDateTime.now())
                .build();

        solutionCommentRepository.save(comment);

        return ru.antonov.oauth2_social.solution.dto.DtoFactory.makeSolutionCommentResponseDto(comment);
    }

    public List<SolutionCommentResponseDto> findAllBySolutionIdSortByPublishedAt(User principal, UUID solutionId){
        Solution solution = solutionService.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Такого решения не существует",
                        String.format("Ошибка при поиске комментариев к решению пользователем %s. " +
                                "Решения %s не существует", principal.getId(), solutionId)
                ));

//        if (solution.getStatus() == SolutionStatus.REVOKED) {
//            throw new AttemptToGetRevokedSolutionEx(
//                    "Ошибка доступа. У вас нет доступа к этому решению",
//                    String.format("Ошибка при поиске комментариев к решению пользователем %s. Решение %s REVOKED",
//                            principal.getId(), solutionId)
//            );
//        }

        return solutionCommentRepository.findAllBySolutionIdSortByPublishedAt(solutionId)
                .stream()
                .map(DtoFactory::makeSolutionCommentResponseDto)
                .toList();
    }

    public void deleteCommentById(User principal, UUID commentId) {
        SolutionComment comment = solutionCommentRepository.findById(commentId)
                .orElseThrow(
                        () -> new EntityNotFoundEx(
                                "Ошибка. Комментарий не найден",
                                String.format(
                                        "Ошибка при удалении комментария пользователем %s. Комментарий %s не найден",
                                        principal.getId(), commentId
                                )
                        )
                );

//        if (comment.getSolution().getStatus() == SolutionStatus.REVOKED) {
//            throw new AttemptToGetRevokedSolutionEx(
//                    "Ошибка доступа. У вас нет доступа к этому решению",
//                    String.format("Ошибка при добавлении комментария к решению пользователем %s. Решение %s REVOKED",
//                            principal.getId(), comment.getSolution().getId())
//            );
//        }

        solutionCommentRepository.deleteById(commentId);
    }
}
