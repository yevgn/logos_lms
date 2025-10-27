package ru.antonov.oauth2_social.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.user.dto.UserShortResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CourseMaterialResponseDto {
    private UUID id;

    private String topic;

    private UserShortResponseDto author;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;

    @JsonProperty("last_changed_at")
    private LocalDateTime lastChangedAt;

    private List<CourseMaterialContentResponseDto> content;
}
