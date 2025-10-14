package ru.antonov.oauth2_social.course.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import ru.antonov.oauth2_social.course.exception.JsonSerializationEx;

import java.util.Map;

public class SolutionContentJsonConverter implements AttributeConverter<Map<String, Object>, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try{
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException ex){
            throw new JsonSerializationEx(
                    "Ошибка на сервере",
                    "Ошибка при преобразовании solutionContent в поле БД в сущности Solution"
            );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try{
            return objectMapper.readValue(dbData, Map.class);
        } catch (JsonProcessingException ex){
            throw new JsonSerializationEx(
                    "Ошибка на сервере",
                    "Ошибка при преобразовании поля БД в solutionContent в сущности Solution"
            );
        }
    }
}
