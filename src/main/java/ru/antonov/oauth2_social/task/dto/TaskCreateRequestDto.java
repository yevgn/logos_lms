package ru.antonov.oauth2_social.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.common.HasContentFiles;
import ru.antonov.oauth2_social.task.validation.TargetUsersNotNullOrEmpty;
import ru.antonov.oauth2_social.task.validation.ToSubmitAtLeastOneHourLater;
import ru.antonov.oauth2_social.common.validation.UniqueFileNames;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@UniqueFileNames
@TargetUsersNotNullOrEmpty
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequestDto implements HasContentFiles {
    @NotBlank(message = "Поле title не может быть пустым или отсутствовать")
    @Size(max = 80, message = "Длина поля title не должна превышать 80 символов")
    private String title;

    @NotBlank(message = "Поле description не может быть пустым или отсутствовать")
    @Size(max = 300, message = "Длина поля description не должна превышать 300 символов")
    private String description;

    @ToSubmitAtLeastOneHourLater
    private LocalDateTime toSubmitAt;

    private boolean assessed = true;

    private boolean forEveryone = true;

    private List<UUID> targetUsersIdList;

    @Size(max = 10, message = "Нельзя загрузить больше 10 файлов за один раз")
    private List<MultipartFile> content;
}
