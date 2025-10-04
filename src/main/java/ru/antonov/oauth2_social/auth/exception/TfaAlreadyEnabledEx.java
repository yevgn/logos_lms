package ru.antonov.oauth2_social.auth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TfaAlreadyEnabledEx extends RuntimeException {
    private String debugMessage;

    public TfaAlreadyEnabledEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
