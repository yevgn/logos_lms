package ru.antonov.oauth2_social.auth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountActivationTokenRenewEx extends RuntimeException {
    private String debugMessage;

    public AccountActivationTokenRenewEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
