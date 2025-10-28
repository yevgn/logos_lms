package ru.antonov.oauth2_social.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.course.validation.TargetUsersNotNullOrEmpty;
import ru.antonov.oauth2_social.course.validation.UniqueFileNames;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@UniqueFileNames
@TargetUsersNotNullOrEmpty
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequestDto {
    @NotBlank(message = "Поле title не может быть пустым или отсутствовать")
    @Size(max = 80, message = "Длина поля title не должна превышать 80 символов")
    private String title;

    @NotBlank(message = "Поле description не может быть пустым или отсутствовать")
    @Size(max = 300, message = "Длина поля description не должна превышать 300 символов")
    private String description;

    @JsonProperty("to_submit_at")
    private LocalDateTime toSubmitAt;

    private boolean isAssessed = true;

    private boolean isForAll = true;

    private List<String> targetUsersIdList;

    @Size(max = 20, message = "Нельзя загрузить больше 20 файлов за один раз")
    private List<MultipartFile> content;
}
