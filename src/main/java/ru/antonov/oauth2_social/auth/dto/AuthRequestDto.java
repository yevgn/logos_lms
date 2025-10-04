package ru.antonov.oauth2_social.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthRequestDto {
    @NotBlank(message = "поле email не должно быть пустым")
    @Pattern(
            regexp = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Неправильный формат email"
    )
    private String email;
    @NotBlank(message = "поле password не должно быть пустым")
    private String password;
//
//    @JsonProperty("client_type")
//    @NotNull(message = "client_type field must not be empty")
//    private ClientType clientType;
}