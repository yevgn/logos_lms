package ru.antonov.oauth2_social.task.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import ru.antonov.oauth2_social.exception.JsonSerializationEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TaskCommentsJsonConverter implements AttributeConverter<List<Task.TaskComment>, String> {
    private final ObjectMapper objectMapper;

    public TaskCommentsJsonConverter() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String convertToDatabaseColumn(List<Task.TaskComment> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationEx(
                    "Ошибка на сервере",
                    "Ошибка при преобразовании taskComments в поле БД в сущности Task:\n" + e.getMessage()
            );
        }
    }

    @Override
    public List<Task.TaskComment> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Task.TaskComment>>() {});
        } catch (IOException e) {
            throw new JsonSerializationEx(
                    "Ошибка на сервере",
                    "Ошибка при преобразовании поля БД в taskComments в сущности Task:\n" + e.getMessage()
            );
        }
    }
}