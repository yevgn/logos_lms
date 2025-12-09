package ru.antonov.oauth2_social.common.exception;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ru.antonov.oauth2_social.config.ApiError;

import java.util.ArrayList;

import java.util.List;

@ControllerAdvice
@Slf4j
public class ExceptionHandler {
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;
    @Value("${spring.servlet.multipart.max-request-size}")
    private String maxRequestSize;


    @org.springframework.web.bind.annotation.ExceptionHandler(
            {MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class}
    )
    public ResponseEntity<ApiError> handle4xxExceptions(Exception ex) {
        log.warn(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiError> handleMissingPathVariableEx(MissingPathVariableException ex) {
        log.warn(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(TaskAndCourseMaterialFileLimitForCourseExceededEx.class)
    public ResponseEntity<ApiError> handleTaskAndMaterialFileLimitExceededEx(TaskAndCourseMaterialFileLimitForCourseExceededEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        log.warn(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Вы отправили данные, формат которых не поддерживается")
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(JsonSerializationEx.class)
    public ResponseEntity<ApiError> handleJsonSerializationEx(JsonSerializationEx ex) {
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EmptyFileEx.class)
    public ResponseEntity<ApiError> handleEmptyFileEx(EmptyFileEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidFileDataFormatEx.class)
    public ResponseEntity<ApiError> handleInvalidFileDataFormatEx(InvalidFileDataFormatEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentEx.class)
    public ResponseEntity<ApiError> handleIllegalArgumentEx(IllegalArgumentEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    @org.springframework.web.bind.annotation.ExceptionHandler(FileNameNotUniqueEx.class)
    public ResponseEntity<ApiError> handleFileNameNotUniqueEx(FileNameNotUniqueEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(FileDuplicatedEx.class)
    public ResponseEntity<ApiError> handleFileDuplicatedEx(FileDuplicatedEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.CONFLICT)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EntityLockEx.class)
    public ResponseEntity<ApiError> handleEntityLockEx(EntityLockEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.CONFLICT)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(FileNotFoundEx.class)
    public ResponseEntity<ApiError> handleFileNotFoundEx(FileNotFoundEx ex) {
        log.warn(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EntityNotFoundEx.class)
    public ResponseEntity<ApiError> handleEntityNotFoundEx(EntityNotFoundEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IOEx.class)
    public ResponseEntity<ApiError> handleIOEx(IOEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(AccessDeniedEx.class)
    public ResponseEntity<ApiError> handlePrincipalNotAttachedToInstitutionEx(AccessDeniedEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.FORBIDDEN)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationEx(ConstraintViolationException ex){
        log.warn("Ошибка при добавлении записи в БД:\n{}", ex.getSQLException().getMessage());
        ApiError apiError = ApiError
                .builder()
                .status(HttpStatus.CONFLICT)
                .message("Нарушены ограничения целостности базы данных. Возможно, вы пытаетесь добавить некорректные данные," +
                        " либо они уже существуют/удалить данные некорректным образом")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(DBConstraintViolationEx.class)
    public ResponseEntity<ApiError> handleConstraintViolationEx(DBConstraintViolationEx ex){
        log.warn(ex.getDebugMessage());
        ApiError apiError = ApiError
                .builder()
                .status(HttpStatus.CONFLICT)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiError> handleNullPointerEx(NullPointerException ex){
        log.error(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка на сервере")
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentEx(IllegalArgumentException ex){
        log.error(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка на сервере")
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MalformedFileUrlEx.class)
    public ResponseEntity<ApiError> handleMalformedFileUrlEx(MalformedFileUrlEx ex){
        log.error(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка на сервере")
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentEx500.class)
    public ResponseEntity<ApiError> handleIllegalArgumentEx(IllegalArgumentEx500 ex){
        log.error(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Ошибка на сервере")
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

    @org.springframework.web.bind.annotation.ExceptionHandler(MailSendEx.class)
    public ResponseEntity<ApiError> handleMailSendException(MailSendEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MailAuthenticationEx.class)
    public ResponseEntity<ApiError> handleMailAuthenticationEx(MailAuthenticationEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(error);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MessagingEx.class)
    public ResponseEntity<ApiError> handleMessagingEx(MessagingEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(error);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(AppConfigurationEx.class)
    public ResponseEntity<ApiError> handleAppConfigurationEx(AppConfigurationEx ex){
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

    @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Неуспешная валидация: ", ex);

        BindingResult bindingResult = ex.getBindingResult();

        List<String> errors = new ArrayList<>();

        bindingResult.getFieldErrors().forEach(error ->
                errors.add(error.getDefaultMessage())
        );

        bindingResult.getGlobalErrors().forEach(error ->
                errors.add(error.getDefaultMessage())
        );

        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(errors.toString())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidationEx(HandlerMethodValidationException ex) {
        log.warn("Неуспешная валидация: ", ex);
        List<String> errors = new ArrayList<>();

        ex.getAllValidationResults()
                .forEach(paramResult ->
                        paramResult.getResolvableErrors()
                                .forEach(err -> errors.add(err.getDefaultMessage()))
                );

        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(errors.toString())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationEx(jakarta.validation.ConstraintViolationException ex) {
        log.warn("Неуспешная валидация: ", ex);
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        ApiError error = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(errors.toString())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("Ошибка загрузки файлов. Превышены параметры maxFileSize, maxRequestSize\n{}", ex.getMessage());
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
}
