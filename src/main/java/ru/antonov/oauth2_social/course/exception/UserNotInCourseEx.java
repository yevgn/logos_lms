package ru.antonov.oauth2_social.course.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserNotInCourseEx extends RuntimeException {
    private String debugMessage;

    public UserNotInCourseEx(String message, String debugMessage) {
        super(message);
        this.debugMessage = debugMessage;
    }
}
