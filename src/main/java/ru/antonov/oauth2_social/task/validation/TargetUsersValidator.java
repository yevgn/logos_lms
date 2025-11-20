package ru.antonov.oauth2_social.task.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.antonov.oauth2_social.task.dto.TaskCreateRequestDto;

public class TargetUsersValidator implements ConstraintValidator<TargetUsersNotNullOrEmpty,
        TaskCreateRequestDto> {

    @Override
    public boolean isValid(TaskCreateRequestDto dto, ConstraintValidatorContext context) {
        if (!dto.isForEveryone()) {
            return dto.getTargetUsersIdList() != null && !dto.getTargetUsersIdList().isEmpty();
        }
        return true;
    }
}
