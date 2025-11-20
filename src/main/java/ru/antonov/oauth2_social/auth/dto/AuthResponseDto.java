package ru.antonov.oauth2_social.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthResponseDto {
    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("is_tfa_enabled")
    private boolean isTfaEnabled;
    @JsonProperty("secret_image_uri")
    private String secretImageUri;

    //private String secret;
}
