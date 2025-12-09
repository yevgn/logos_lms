package ru.antonov.oauth2_social.task.dto;


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
public class TaskShortResponseDto {
    private UUID id;
    private String title;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;
    @JsonProperty("last_changed_at")
    private LocalDateTime lastChangedAt;
    @JsonProperty("to_submit_at")
    private LocalDateTime toSubmitAt;

    private UserShortResponseDto author;

    @JsonProperty("is_assessed")
    private boolean isAssessed;

    @JsonProperty("is_for_everyone")
    private boolean isForEveryone;
}
