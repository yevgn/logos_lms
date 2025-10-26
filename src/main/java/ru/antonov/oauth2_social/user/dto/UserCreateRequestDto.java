package ru.antonov.oauth2_social.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.antonov.oauth2_social.user.entity.Role;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequestDto {
    @NotBlank(message = "поле surname не должно быть пустым")
    @Size(max = 30, message = "Длина surname не может превышать 30 символов")
    @Pattern(
            regexp = "^[а-яА-Я]{2,}$",
            message = "Неправильный формат фамилии"
    )
    private String surname;

    @NotBlank(message = "поле name не должно быть пустым")
    @Size(max = 30, message = "Длина name не может превышать 30 символов")
    @Pattern(
            regexp = "^[а-яА-Я]{2,}$",
            message = "Неправильный формат имени"
    )
    private String name;

    @Size(max = 30, message = "Длина patronymic не может превышать 30 символов")
    @Pattern(
            regexp = "^[а-яА-Я]{2,}$",
            message = "Неправильный формат отчества"
    )
    private String patronymic;

    @NotNull(message = "поле role не должно отсутствовать")
    private Role role;

    @NotBlank(message = "Поле email не может быть пустым")
    @Size(max = 50, message = "Длина email не может превышать 50 символов")
    @Pattern(
            regexp = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Неправильный формат email"
    )
    private String email;

    @Min(value = 5, message = "Минимальное значение age - 5")
    @Max(value = 120, message = "Максимальное значение age - 120")
    private Integer age;

    @Size(max = 20, message = "Длина group_name не может превышать 20 символов")
    @JsonProperty("group_name")
    private String groupName;
}