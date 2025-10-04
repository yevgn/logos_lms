package ru.antonov.oauth2_social.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailSendEx extends RuntimeException {
    private String debugMessage;

    public MailSendEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
