package ru.antonov.oauth2_social.course.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import ru.antonov.oauth2_social.course.exception.JsonSerializationEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CourseMaterialContentJsonConverter implements AttributeConverter<List<CourseMaterialContent>, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<CourseMaterialContent> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationEx(
                    "Ошибка на сервере",
                    "Ошибка при преобразовании courseMaterialContent в поле БД в сущности CourseMaterial"
            );
        }
    }

    @Override
    public List<CourseMaterialContent> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(dbData, new TypeReference<List<CourseMaterialContent>>() {});
        } catch (IOException e) {
            throw new JsonSerializationEx(
                    "Ошибка на сервере",
                    "Ошибка при преобразовании courseMaterialContent в поле БД в сущности CourseMaterial"
            );
        }
    }

}