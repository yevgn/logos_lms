package ru.antonov.oauth2_social.solution.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.antonov.oauth2_social.config.ApiError;

@ControllerAdvice
@Slf4j
public class SolutionControllerExceptionHandler {
    @ExceptionHandler(SolutionFileLimitForCourseExceededEx.class)
    public ResponseEntity<ApiError> handleSolutionFileLimitForCourseExceededEx(SolutionFileLimitForCourseExceededEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SolutionFileLimitExceededEx.class)
    public ResponseEntity<ApiError> handleSolutionFileLimitExceededEx(SolutionFileLimitExceededEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SolutionAlreadyReviewedEx.class)
    public ResponseEntity<ApiError> handleSolutionAlreadyReviewedEx(SolutionAlreadyReviewedEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SolutionReviewMarkMissingEx.class)
    public ResponseEntity<ApiError> handleSolutionReviewMarkMissingEx(SolutionReviewMarkMissingEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AttemptToGetRevokedSolutionEx.class)
    public ResponseEntity<ApiError> handleAttemptToGetRevokedSolutionEx(AttemptToGetRevokedSolutionEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
