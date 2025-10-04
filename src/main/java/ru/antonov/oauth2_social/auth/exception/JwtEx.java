package ru.antonov.oauth2_social.auth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtEx extends RuntimeException {
    private String debugMessage;

    public JwtEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
