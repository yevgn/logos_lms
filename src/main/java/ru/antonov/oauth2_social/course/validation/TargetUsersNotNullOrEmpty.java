package ru.antonov.oauth2_social.course.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TargetUsersValidator.class)
@Documented
public @interface TargetUsersNotNullOrEmpty {
    String message() default "Если isForAll = false, то targetUsersIdList не должен быть пустым";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
