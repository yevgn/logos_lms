package ru.antonov.oauth2_social.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateWithGroupIdListRequestDto {
    @NotBlank(message = "Поле name не может отсутствовать")
    @Size(min = 3, max = 255, message = "Длина поля name должна быть больше 2 и не превышать 255 символов")
    private String name;

    @JsonProperty("group_id_list")
    private List<UUID> groupIdList;
}
