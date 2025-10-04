package ru.antonov.oauth2_social.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IOEx extends RuntimeException {
    private String debugMessage;

    public IOEx(String message, String debugMessage) {
        super(message);
    }
}
