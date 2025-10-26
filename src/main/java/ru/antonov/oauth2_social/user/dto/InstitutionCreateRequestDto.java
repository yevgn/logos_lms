package ru.antonov.oauth2_social.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.antonov.oauth2_social.user.entity.InstitutionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionCreateRequestDto {
    @NotBlank(message = "Поле email не может быть пустым")
    @Size(max = 50, message = "Длина email не может превышать 50 символов")
    @Pattern(
            regexp = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Неправильный формат email"
    )
    private String email;

    @NotBlank(message = "Поле short_name не может быть пустым")
    @Size(max = 20, message = "Размер поля short_name не может превышать 20")
    @JsonProperty("short_name")
    private String shortName;

    @NotBlank(message = "Поле full_name не может быть пустым")
    @Size(max = 70, message = "Размер поля full_name не может превышать 70")
    @JsonProperty("full_name")
    private String fullName;

    @NotBlank(message = "Поле location не может быть пустым")
    @Size(max = 70, message = "Размер поля location не может превышать 70")
    private String location;

    @NotNull(message = "Поле institution_type не может отсутствовать")
    @JsonProperty("institution_type")
    private InstitutionType institutionType;
}
