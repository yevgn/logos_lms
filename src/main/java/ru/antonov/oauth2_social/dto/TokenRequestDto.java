package ru.antonov.oauth2_social.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.entity.TokenMode;

@Builder
@Getter
@Setter
public class TokenRequestDto {
    @NotBlank(message = "Поле token отсутствует или является пустым")
    private String token;

    @NotNull(message = "Поле token_mode отсутствует")
    @JsonProperty("token_mode")
    private TokenMode tokenMode;
}
