package ru.antonov.oauth2_social.user.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.antonov.oauth2_social.config.ApiError;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;
    @Value("${spring.servlet.multipart.max-request-size}")
    private String maxRequestSize;

    @ExceptionHandler(EmptyFileEx.class)
    public ResponseEntity<ApiError> handleEmptyFileEx(EmptyFileEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("Ошибка загрузки файлов. Превышены параметры maxFileSize, maxRequestSize\n" + ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(
                        String.format("Ошибка при загрузке файлов. Вы пытаетесь загрузить слишком большие файлы." +
                                " Максимальный размер одного файла - %s. Максимальный общий размер файлов в " +
                                "одном запросе - %s ", maxFileSize, maxRequestSize)
                )
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

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

    @ExceptionHandler(InvalidFileDataFormatEx.class)
    public ResponseEntity<ApiError> handleInvalidFileDataFormatEx(InvalidFileDataFormatEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccountActivationTokenRenewEx.class)
    public ResponseEntity<ApiError> handleAccountActivationTokenRenewEx(AccountActivationTokenRenewEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.OK)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.OK);
    }


}
