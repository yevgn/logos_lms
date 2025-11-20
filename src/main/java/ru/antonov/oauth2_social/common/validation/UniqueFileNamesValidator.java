package ru.antonov.oauth2_social.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import ru.antonov.oauth2_social.common.HasContentFiles;

import java.util.HashSet;
import java.util.Set;

public class UniqueFileNamesValidator implements ConstraintValidator<UniqueFileNames, HasContentFiles> {

    @Override
    public boolean isValid(HasContentFiles dto, ConstraintValidatorContext context) {
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