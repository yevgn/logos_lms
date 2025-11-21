package ru.antonov.oauth2_social.solution.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.antonov.oauth2_social.solution.entity.SolutionStatus;
import ru.antonov.oauth2_social.task.dto.TaskShortResponseDto;


import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
public class SolutionWithTaskShortResponseDto {
    private UUID id;

    private Integer mark;

    private SolutionStatus status;

    @JsonProperty("submitted_at")
    private LocalDateTime submittedAt;

    @JsonProperty("reviewed_at")
    private LocalDateTime reviewedAt;

    private TaskShortResponseDto task;
}
