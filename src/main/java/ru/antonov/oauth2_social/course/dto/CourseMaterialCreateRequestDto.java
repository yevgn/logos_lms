package ru.antonov.oauth2_social.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.common.HasContentFiles;
import ru.antonov.oauth2_social.common.validation.UniqueFileNames;

import java.util.List;

@Data
@Builder
@UniqueFileNames
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterialCreateRequestDto implements HasContentFiles {
    @NotBlank(message = "Поле topic не может отсутствовать или быть пустым")
    @Size(max = 100, message = "максимальная длина поля topic - 100")
    private String topic;

    @Size(max = 20, message = "Нельзя загрузить больше 20 файлов за один раз")
    @NotEmpty(message = "Поле content не может быть пустым")
    private List<MultipartFile> content;
}
