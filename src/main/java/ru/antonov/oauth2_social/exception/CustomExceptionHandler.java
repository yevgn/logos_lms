package ru.antonov.oauth2_social.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;


import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ru.antonov.oauth2_social.config.ApiError;

import java.util.ArrayList;

import java.util.List;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler{
    @ExceptionHandler(
            {MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, NoResourceFoundException.class}
    )
    public ResponseEntity<ApiError> handle4xxExceptions(Exception ex) {
        log.warn(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundEx.class)
    public ResponseEntity<ApiError> handleEntityNotFoundEx(EntityNotFoundEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IOEx.class)
    public ResponseEntity<ApiError> handleIOEx(IOEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedEx.class)
    public ResponseEntity<ApiError> handlePrincipalNotAttachedToInstitutionEx(AccessDeniedEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.FORBIDDEN)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ApiError> handleConstraintViolationEx(ConstraintViolationException ex){
//        log.warn("Ошибка при добавлении записи в БД:\n" + ex.getSQLException().getMessage());
//        ApiError apiError = ApiError
//                .builder()
//                .status(HttpStatus.CONFLICT)
//                .error("Нарушены ограничения целостности базы данных. Возможно, вы пытаетесь добавить некорректные данные," +
//                        " либо они уже существуют")
//                .build();
//        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
//    }

    @ExceptionHandler(DBConstraintViolationEx.class)
    public ResponseEntity<ApiError> handleConstraintViolationEx(DBConstraintViolationEx ex){
        log.warn(ex.getDebugMessage());
        ApiError apiError = ApiError
                .builder()
                .status(HttpStatus.CONFLICT)
                .error(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiError> handleNullPointerEx(NullPointerException ex){
        log.error(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .error("Ошибка на сервере")
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentEx(IllegalArgumentException ex){
        log.error(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .error("Ошибка на сервере")
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }



//    @ExceptionHandler(MailAuthenticationException.class)
//    public ResponseEntity<ApiError> handleMailAuthenticationEx(MailAuthenticationException ex){
//        log.error("Ошибка при отправке письма по SMTP: неудачная аутентификация\n" + ex.getMessage());
//        ApiError error = ApiError
//                .builder()
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .message("Ошибка на сервере")
//                .build();
//        return ResponseEntity.internalServerError().body(error);
//    }

//    @ExceptionHandler(MailSendException.class)
//    public ResponseEntity<ApiError> handleMailSendException(MailSendException ex){
//        log.info("Ошибка при отправке письма по SMTP\n" + ex.getMessage());
//        ApiError error = ApiError
//                .builder()
//                .status(HttpStatus.BAD_GATEWAY)
//                .message("Ошибка при отправке электронного письма")
//                .build();
//        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
//    }

    @ExceptionHandler(MailSendEx.class)
    public ResponseEntity<ApiError> handleMailSendException(MailSendEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MailAuthenticationEx.class)
    public ResponseEntity<ApiError> handleMailAuthenticationEx(MailAuthenticationEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(error);
    }

    @ExceptionHandler(MessagingEx.class)
    public ResponseEntity<ApiError> handleMessagingEx(MessagingEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(error);
    }

//    @ExceptionHandler(MessagingException.class)
//    public ResponseEntity<ApiError> handleMessagingException(MessagingException ex){
//        log.error("Ошибка при создании mimeMessage\n" + ex.getMessage());
//        ApiError error = ApiError
//                .builder()
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .message("Ошибка на сервере")
//                .build();
//        return ResponseEntity.internalServerError().body(error);
//    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Неуспешная валидация: ", ex);
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.add(error.getDefaultMessage())
        );

        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .error(errors.toString())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidationEx(HandlerMethodValidationException ex) {
        log.warn("Неуспешная валидация: ", ex);
        List<String> errors = new ArrayList<>();

        ex.getParameterValidationResults().forEach(validationRes ->
                validationRes.getResolvableErrors().forEach( error ->
                        errors.add(error.getDefaultMessage())
                ));

        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .error(errors.toString())
                .build();

        return ResponseEntity.badRequest().body(error);
    }
}
