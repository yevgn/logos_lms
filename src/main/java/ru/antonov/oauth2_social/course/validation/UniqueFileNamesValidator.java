package ru.antonov.oauth2_social.course.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.course.dto.CourseMaterialCreateRequestDto;

import java.util.HashSet;
import java.util.Set;

public class UniqueFileNamesValidator implements ConstraintValidator<UniqueFileNames, CourseMaterialCreateRequestDto> {

    @Override
    public boolean isValid(CourseMaterialCreateRequestDto dto, ConstraintValidatorContext context) {
        if (dto.getContent() == null) {
            return true;
        }

        Set<String> names = new HashSet<>();
        for (MultipartFile file : dto.getContent()) {
            if (!names.add(file.getOriginalFilename())) {
                return false;
            }
        }
        return true;
    }
}