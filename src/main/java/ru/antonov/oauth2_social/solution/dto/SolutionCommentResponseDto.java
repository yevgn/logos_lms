package ru.antonov.oauth2_social.solution.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
public class SolutionCommentResponseDto {
    private UUID id;
    private UserShortResponseDto author;
    @JsonProperty("published_at")
    private LocalDateTime publishedAt;
    private String text;
}
