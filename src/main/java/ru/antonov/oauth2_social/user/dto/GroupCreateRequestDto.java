package ru.antonov.oauth2_social.user.dto;

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
public class GroupCreateRequestDto {
    @NotBlank(message = "поле name не должно быть пустым")
    @Size(max = 20, message = "Длина name не может превышать 20 символов")
    private String name;
}
