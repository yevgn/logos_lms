package ru.antonov.oauth2_social.task.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.common.ContentFileResponseDto;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TaskResponseDto {
    private UUID id;
    private String title;
    private String description;

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

    @JsonProperty("target_user_list")
    private List<UserShortResponseDto> targetUserList;

    private List<ContentFileResponseDto> content;
}
