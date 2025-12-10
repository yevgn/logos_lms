package ru.antonov.oauth2_social.course.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseMaterialAmountForCourseExceedsLimitEx extends RuntimeException {
    private String debugMessage;

    public CourseMaterialAmountForCourseExceedsLimitEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
