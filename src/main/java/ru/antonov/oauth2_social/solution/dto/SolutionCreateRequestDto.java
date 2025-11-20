package ru.antonov.oauth2_social.solution.dto;

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
@NoArgsConstructor
@AllArgsConstructor
@UniqueFileNames
public class SolutionCreateRequestDto implements HasContentFiles {
    @Size(max = 5, message = "Нельзя загрузить больше 5 файлов за один раз")
    @NotEmpty(message = "Поле content не может быть пустым")
    private List<MultipartFile> content;
}
