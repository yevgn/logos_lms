package ru.antonov.oauth2_social.auth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Qr2faGenerationEx extends RuntimeException {
    private String debugMessage;

    public Qr2faGenerationEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
