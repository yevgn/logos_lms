package ru.antonov.oauth2_social.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessDeniedEx extends RuntimeException {
    private String debugMessage;

    public AccessDeniedEx( String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
