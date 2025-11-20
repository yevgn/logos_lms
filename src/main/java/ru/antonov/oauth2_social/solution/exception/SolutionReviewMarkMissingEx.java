package ru.antonov.oauth2_social.solution.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolutionReviewMarkMissingEx extends RuntimeException {
    private String debugMessage;

    public SolutionReviewMarkMissingEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
