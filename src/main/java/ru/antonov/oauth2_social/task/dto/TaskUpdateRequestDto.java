package ru.antonov.oauth2_social.task.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import ru.antonov.oauth2_social.common.HasContentFiles;
import ru.antonov.oauth2_social.task.validation.ToSubmitAtLeastOneHourLater;
import ru.antonov.oauth2_social.common.validation.UniqueFileNames;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@UniqueFileNames
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequestDto implements HasContentFiles {
    @Pattern(regexp = ".*\\S.*", message = "Поле newTitle не может быть пустым или состоять только из пробелов")
    private String newTitle;

    @Pattern(regexp = ".*\\S.*", message = "Поле newDescription не может быть пустым или состоять только из пробелов")
    private String newDescription;

    @ToSubmitAtLeastOneHourLater
    private LocalDateTime newToSubmitAt;

    @Size(max = 10, message = "Нельзя загрузить больше 10 файлов за один раз")
    private List<MultipartFile> content;

    @Size(max = 10, message = "максимальный размер toDeleteList - 10")
    private List<UUID> toDeleteList;
}
