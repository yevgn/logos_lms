package ru.antonov.oauth2_social.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.antonov.oauth2_social.auth.entity.TokenMode;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequestDto {
    @NotBlank(message = "Поле token отсутствует или является пустым")
    private String token;

    @NotNull(message = "Поле token_mode отсутствует")
    @JsonProperty("token_mode")
    private TokenMode tokenMode;
}
