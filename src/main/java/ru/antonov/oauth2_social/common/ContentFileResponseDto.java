package ru.antonov.oauth2_social.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class ContentFileResponseDto {
    private UUID id;
    @JsonProperty("original_file_name")
    private String originalFileName;
}
