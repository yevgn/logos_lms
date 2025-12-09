package ru.antonov.oauth2_social.common.exception;

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
