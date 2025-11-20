package ru.antonov.oauth2_social.task.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ToSubmitAtLeastOneHourLaterValidator.class)
public @interface ToSubmitAtLeastOneHourLater {

    String message() default "Время дедлайна должно быть минимум на 1 час позже текущего";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}