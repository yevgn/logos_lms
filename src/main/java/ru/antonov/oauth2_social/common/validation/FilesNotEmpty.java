package ru.antonov.oauth2_social.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FilesNotEmptyValidator.class)
public @interface FilesNotEmpty {
    String message() default "Нельзя загружать пустые файлы";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}