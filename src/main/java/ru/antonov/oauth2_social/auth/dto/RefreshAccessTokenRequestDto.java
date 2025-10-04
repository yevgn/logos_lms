package ru.antonov.oauth2_social.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshAccessTokenRequestDto {
    @NotBlank(message = "поле refresh_token не должно быть пустым")
    @JsonProperty("refresh_token")
    private String refreshToken;
}
