package ru.antonov.oauth2_social.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.common.HasContentFiles;

public class FilesNotEmptyValidator implements ConstraintValidator<FilesNotEmpty, HasContentFiles> {

    @Override
    public boolean isValid(HasContentFiles dto, ConstraintValidatorContext context) {
        if (dto.getContent() == null) {
            return true;
        }

        for (MultipartFile file : dto.getContent()) {
            if(file.isEmpty()){
                return false;
            }
        }

        return true;
    }
}
