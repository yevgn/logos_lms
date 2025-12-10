package ru.antonov.oauth2_social.task.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskAmountForCourseExceedsLimitEx extends RuntimeException {
    private String debugMessage;

    public TaskAmountForCourseExceedsLimitEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
