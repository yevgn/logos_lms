package ru.antonov.oauth2_social.auth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TfaNotEnabledEx extends RuntimeException {
    private String debugMessage;

    public TfaNotEnabledEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
