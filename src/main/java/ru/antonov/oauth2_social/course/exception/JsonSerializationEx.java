package ru.antonov.oauth2_social.course.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonSerializationEx extends RuntimeException {
    private String debugMessage;

    public JsonSerializationEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
