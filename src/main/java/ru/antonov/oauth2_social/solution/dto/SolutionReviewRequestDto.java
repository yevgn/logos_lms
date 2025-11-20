package ru.antonov.oauth2_social.solution.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionReviewRequestDto {
    @NotNull(message = "Поле status не должно отсутствовать")
    private SolutionReviewStatus status;

    @Min(value = 0, message = "Минимальное значение mark - 0")
    @Max(value = 100, message = "Максимальное значение mark - 100")
    private Integer mark;
}
