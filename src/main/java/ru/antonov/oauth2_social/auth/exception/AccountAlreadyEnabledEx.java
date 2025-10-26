package ru.antonov.oauth2_social.auth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountAlreadyEnabledEx extends RuntimeException {
    private String debugMessage;

    public AccountAlreadyEnabledEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
