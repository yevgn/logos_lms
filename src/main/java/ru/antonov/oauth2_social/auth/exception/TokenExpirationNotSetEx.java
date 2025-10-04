package ru.antonov.oauth2_social.auth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenExpirationNotSetEx extends RuntimeException {
    private String debugMessage;

    public TokenExpirationNotSetEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
