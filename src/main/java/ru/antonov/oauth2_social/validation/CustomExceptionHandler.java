package ru.antonov.oauth2_social.validation;

import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.log4j.Log4j2;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;

import java.util.List;

@ControllerAdvice
@Log4j2
public class CustomExceptionHandler{

    @ExceptionHandler({ ClientNotFoundException.class, TokenConfigurationException.class, InvalidStateException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, NoResourceFoundException.class, EntityNotFoundException.class,
            SecuredEndpointAccessEx.class, JwtException.class} )
    public ResponseEntity<ApiError> handle4xxExceptions(Exception ex, WebRequest request) throws Exception {
        log.error("Error: ", ex);
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OAuth2AuthorizationException.class)
    public ResponseEntity<ApiError> handleOAuth2AuthEx(OAuth2AuthorizationException ex){
        log.error("Error: ", ex);
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_GATEWAY)
                .error(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationEx(ConstraintViolationException ex){
        log.error("Error: ", ex);
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.CONFLICT)
                .error(ex.getSQLException().getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(AccountInactiveEx.class)
    public ResponseEntity<?> handle403Exception(AccountInactiveEx ex){
        log.error("Error: ", ex);
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.FORBIDDEN)
                .error(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorList> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Error: ", ex);
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.add(error.getDefaultMessage())
        );

        ApiErrorList error = ApiErrorList
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }


    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorList> handleHandlerMethodValidationEx(HandlerMethodValidationException ex) {
        log.error("Error: ", ex);
        List<String> errors = new ArrayList<>();

        ex.getParameterValidationResults().forEach(validationRes ->
                validationRes.getResolvableErrors().forEach( error ->
                        errors.add(error.getDefaultMessage())
                ));

        ApiErrorList error = ApiErrorList
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }
}
