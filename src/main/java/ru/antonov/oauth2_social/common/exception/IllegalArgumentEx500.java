package ru.antonov.oauth2_social.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IllegalArgumentEx500 extends RuntimeException {
    private String debugMessage;

    public IllegalArgumentEx500(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
