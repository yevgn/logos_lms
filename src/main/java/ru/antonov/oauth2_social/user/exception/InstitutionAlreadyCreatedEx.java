package ru.antonov.oauth2_social.user.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstitutionAlreadyCreatedEx extends RuntimeException {
    private String debugMessage;

    public InstitutionAlreadyCreatedEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
