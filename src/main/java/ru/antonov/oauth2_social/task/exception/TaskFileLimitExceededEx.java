package ru.antonov.oauth2_social.task.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskFileLimitExceededEx extends RuntimeException {
    private String debugMessage;

    public TaskFileLimitExceededEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
