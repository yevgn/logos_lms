package ru.antonov.oauth2_social.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CourseCreateWithUserIdListRequestDto {
    @NotBlank(message = "Поле name не может отсутствовать")
    @Size(min = 3, max = 255, message = "Длина поля name должна быть больше 2 и не превышать 255 символов")
    private String name;

    @JsonProperty("user_id_list")
    private List<UUID> userIdList;
}