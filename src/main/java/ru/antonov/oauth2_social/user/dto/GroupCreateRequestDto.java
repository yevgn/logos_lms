package ru.antonov.oauth2_social.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupCreateRequestDto {
    @NotBlank(message = "поле name не должно быть пустым")
    @Size(max = 20, message = "Длина group не может превышать 20 символов")
    private String name;
}
