package ru.antonov.oauth2_social.user.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.antonov.oauth2_social.config.ApiError;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {
    @ExceptionHandler(EmptyFileEx.class)
    public ResponseEntity<ApiError> handleEmptyFileEx(EmptyFileEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidFileDataFormatEx.class)
    public ResponseEntity<ApiError> handleInvalidFileDataFormatEx(InvalidFileDataFormatEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccountActivationTokenRenewEx.class)
    public ResponseEntity<ApiError> handleAccountActivationTokenRenewEx(AccountActivationTokenRenewEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.OK)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.OK);
    }


}
