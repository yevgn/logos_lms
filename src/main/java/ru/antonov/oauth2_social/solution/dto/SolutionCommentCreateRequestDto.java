package ru.antonov.oauth2_social.solution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionCommentCreateRequestDto {
    @NotBlank(message = "Поле text не должно отсутствовать или быть пустым")
    @Size(max = 300, message = "Максимальная длина поля text - 300")
    private String text;
}
