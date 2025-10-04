package ru.antonov.oauth2_social.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.user.entity.Role;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsvParser {
    private final Validator validator;

    public List<UserCsv> parseCsv(MultipartFile file) throws IOException, IllegalArgumentException {
        List<UserCsv> users = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), Charset.forName("Windows-1251")));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            checkHeadersExistOrElseThrow(headerMap);

            Iterable<CSVRecord> csvRecords = csvParser.getRecords();

            for (CSVRecord csvRecord : csvRecords) {

                Role role;
                if (csvRecord.get("role").equalsIgnoreCase("студент")) {
                    role = Role.STUDENT;
                } else if (csvRecord.get("role").equalsIgnoreCase("преподаватель")) {
                    role = Role.TUTOR;
                } else {
                    throw new IllegalArgumentException("Некорректный формат данных");
                }

                UserCsv user = UserCsv
                        .builder()
                        .name(csvRecord.get("name"))
                        .surname(csvRecord.get("surname"))
                        .patronymic((csvRecord.get("patronymic").isBlank() ? null : csvRecord.get("patronymic")))
                        .email(csvRecord.get("email"))
                        .groupName(csvRecord.get("group").isBlank() ? null : csvRecord.get("group"))
                        .age(csvRecord.get("age").isBlank() ? null : Integer.parseInt(csvRecord.get("age")))
                        .role(role)
                        .build();

                Set<ConstraintViolation<UserCsv>> violations = validator.validate(user);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
                users.add(user);
            }

            return users;
        }
    }


    private void checkHeadersExistOrElseThrow(Map<String, Integer> csvHeaders) {
        if (!csvHeaders.containsKey("name")) {
            throw new IllegalArgumentException("CSV документ не содержит обязательного поля name");
        } else if (!csvHeaders.containsKey("surname")) {
            throw new IllegalArgumentException("CSV документ не содержит обязательного поля surname");
        } else if (!csvHeaders.containsKey("patronymic")) {
            throw new IllegalArgumentException("CSV документ не содержит обязательного поля patronymic");
        } else if (!csvHeaders.containsKey("email")) {
            throw new IllegalArgumentException("CSV документ не содержит обязательного поля email");
        } else if (!csvHeaders.containsKey("group")) {
            throw new IllegalArgumentException("CSV документ не содержит обязательного поля group");
        } else if (!csvHeaders.containsKey("age")) {
            throw new IllegalArgumentException("CSV документ не содержит обязательного поля age");
        } else if (!csvHeaders.containsKey("role")) {
            throw new IllegalArgumentException("CSV документ не содержит обязательного поля role");
        }

    }

    @Data
    @Builder
    @Valid
    public static class UserCsv {

        @NotBlank(message = "Поле email не может быть пустым")
        @Size(max = 50, message = "Длина email не может превышать 50 символов")
        @Pattern(
                regexp = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Неправильный формат email"
        )
        private String email;

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

        @Size(max = 30, message = "Длина patronymic не может превышать 30 символов ")
        @Pattern(
                regexp = "^[а-яА-Я]{2,}$",
                message = "Неправильный формат отчества"
        )
        private String patronymic;

        @Min(value = 5, message = "Минимальное значение age - 5")
        @Max(value = 120, message = "Максимальное значение age - 120")
        private Integer age;

        @Size(min = 1, max = 20, message = "Длина group_name не может превышать 20 символов и должна иметь хотя бы один")
        @JsonProperty("group_name")
        private String groupName;

        @NotNull(message = "поле role не должно отсутствовать")
        private Role role;
    }

}
