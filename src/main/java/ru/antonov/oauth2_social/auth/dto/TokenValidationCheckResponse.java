package ru.antonov.oauth2_social.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class TokenValidationCheckResponse {
    @JsonProperty("is_valid")
    private boolean isValid;
}
