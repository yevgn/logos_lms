package ru.antonov.oauth2_social.task.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class ToSubmitAtLeastOneHourLaterValidator
        implements ConstraintValidator<ToSubmitAtLeastOneHourLater, LocalDateTime> {

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDateTime nowPlusOneHour = LocalDateTime.now().plusHours(1);
        return value.isAfter(nowPlusOneHour);
    }
}