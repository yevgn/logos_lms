package ru.antonov.oauth2_social.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntityLockEx extends RuntimeException {
    private String debugMessage;

    public EntityLockEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
