package ru.antonov.oauth2_social.task.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IllegalTaskDeadlineEx extends RuntimeException {
    private String debugMessage;

    public IllegalTaskDeadlineEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
