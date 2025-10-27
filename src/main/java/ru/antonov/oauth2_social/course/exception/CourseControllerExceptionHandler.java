package ru.antonov.oauth2_social.course.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import ru.antonov.oauth2_social.config.ApiError;
import ru.antonov.oauth2_social.exception.UserAlreadyJoinedCourseEx;

@ControllerAdvice
@Slf4j
public class CourseControllerExceptionHandler {
    @ExceptionHandler(JsonSerializationEx.class)
    public ResponseEntity<ApiError> handleJsonSerializationEx(JsonSerializationEx ex) {
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalTaskDeadlineEx.class)
    public ResponseEntity<ApiError> handleIllegalTaskDeadlineEx(IllegalTaskDeadlineEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyJoinedCourseEx.class)
    public ResponseEntity<ApiError> handleUserAlreadyJoinedCourseEx(UserAlreadyJoinedCourseEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TaskAndMaterialFileLimitExceededEx.class)
    public ResponseEntity<ApiError> handleTaskAndMaterialFileLimitExceededEx(TaskAndMaterialFileLimitExceededEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAmountForCourseLimitExceededEx.class)
    public ResponseEntity<ApiError> handleUserAmountForCourseLimitExceededEx(UserAmountForCourseLimitExceededEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CourseMaterialFileLimitExceededEx.class)
    public ResponseEntity<ApiError> handleCourseMaterialFileLimitExceededEx(CourseMaterialFileLimitExceededEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(FileNameNotUniqueEx.class)
    public ResponseEntity<ApiError> handleFileNameNotUniqueEx(FileNameNotUniqueEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}