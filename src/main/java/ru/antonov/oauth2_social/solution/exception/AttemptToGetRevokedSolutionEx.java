package ru.antonov.oauth2_social.solution.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttemptToGetRevokedSolutionEx extends RuntimeException {
    private String debugMessage;

    public AttemptToGetRevokedSolutionEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
