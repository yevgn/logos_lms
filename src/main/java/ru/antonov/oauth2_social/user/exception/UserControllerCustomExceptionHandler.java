package ru.antonov.oauth2_social.user.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.antonov.oauth2_social.config.ApiError;

@ControllerAdvice
@Slf4j
public class UserControllerCustomExceptionHandler {
    @ExceptionHandler(InstitutionAlreadyCreatedEx.class)
    public ResponseEntity<ApiError> handleInstitutionAlreadyCreatedEx(InstitutionAlreadyCreatedEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
