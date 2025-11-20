package ru.antonov.oauth2_social.solution.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolutionAlreadyReviewedEx extends RuntimeException {
    private String debugMessage;

    public SolutionAlreadyReviewedEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
