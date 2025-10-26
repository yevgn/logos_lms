package ru.antonov.oauth2_social.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshAccessTokenRequestDto {
    @NotBlank(message = "поле refresh_token не должно быть пустым")
    @JsonProperty("refresh_token")
    private String refreshToken;
}
