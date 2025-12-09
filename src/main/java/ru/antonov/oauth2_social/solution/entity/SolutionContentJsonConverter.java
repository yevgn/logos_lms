package ru.antonov.oauth2_social.solution.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import ru.antonov.oauth2_social.common.Content;
import ru.antonov.oauth2_social.common.exception.JsonSerializationEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SolutionContentJsonConverter implements AttributeConverter<List<Content>, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Content> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationEx(
                    "Ошибка на сервере",
                    "Ошибка при преобразовании solutionContent в поле БД в сущности Solution"
            );
        }
    }

    @Override
    public List<Content> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Content>>() {});
        } catch (IOException e) {
            throw new JsonSerializationEx(
                    "Ошибка на сервере",
                    "Ошибка при преобразовании поля БД в solutionContent в сущности Solution"
            );
        }
    }
}
