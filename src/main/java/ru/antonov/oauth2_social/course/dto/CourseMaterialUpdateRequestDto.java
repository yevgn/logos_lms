package ru.antonov.oauth2_social.course.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.course.validation.UniqueFileNames;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@UniqueFileNames
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterialUpdateRequestDto implements HasContentFiles{
    @Pattern(regexp = ".*\\S.*", message = "Поле topic не может быть пустым или состоять только из пробелов")
    private String newTopic;

    @Size(max = 20, message = "Нельзя загрузить больше 20 файлов за один раз")
    private List<MultipartFile> content;

    @Size(max = 20, message = "максимальный размер toDeleteList - 20")
    private List<UUID> toDeleteList;
}
