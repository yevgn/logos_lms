package ru.antonov.oauth2_social.course.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.antonov.oauth2_social.course.dto.TaskCreateRequestDto;

public class TargetUsersValidator implements ConstraintValidator<TargetUsersNotNullOrEmpty,
        TaskCreateRequestDto> {

    @Override
    public boolean isValid(TaskCreateRequestDto dto, ConstraintValidatorContext context) {
        if (!dto.isForAll()) {
            return dto.getTargetUsersIdList() != null && !dto.getTargetUsersIdList().isEmpty();
        }
        return true;
    }
}
