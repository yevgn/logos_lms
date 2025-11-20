package ru.antonov.oauth2_social.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.antonov.oauth2_social.config.ApiError;

@ControllerAdvice
@Slf4j
public class AuthControllerExceptionHandler {
    @ExceptionHandler(BadTfaCodeEx.class)
    public ResponseEntity<ApiError> handleBadTfaCodeEx(BadTfaCodeEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.UNAUTHORIZED)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
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

    @ExceptionHandler(JwtEx.class)
    public ResponseEntity<ApiError> handleJwtEx(JwtEx ex) {
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenUserMismatchEx.class)
    public ResponseEntity<ApiError> handleTokenUserMismatchEx(TokenUserMismatchEx ex){
        log.warn(ex.getDebugMessage());
        ApiError info = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.badRequest().body(info);
    }

    @ExceptionHandler(AccountAlreadyEnabledEx.class)
    public ResponseEntity<ApiError> handleAccountAlreadyEnabledEx(AccountAlreadyEnabledEx ex){
        log.warn(ex.getDebugMessage());
        ApiError info = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.badRequest().body(info);
    }

    @ExceptionHandler(ResetPasswordTokenRenewEx.class)
    public ResponseEntity<ApiError> handleResetPasswordTokenRenewEx(ResetPasswordTokenRenewEx ex){
        log.warn(ex.getDebugMessage());
        ApiError info = ApiError
                .builder()
                .status(HttpStatus.OK)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.ok().body(info);
    }

    @ExceptionHandler(TokenExpirationNotSetEx.class)
    public ResponseEntity<ApiError> handleTokenExpirationNotSetEx(TokenExpirationNotSetEx ex){
        log.error(ex.getDebugMessage());
        ApiError info = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(info);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabledEx(DisabledException ex) {
        log.warn(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.UNAUTHORIZED)
                .message("Аутентификация не пройдена. Адрес вашей эл. почты не подтвержден")
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundEx(UsernameNotFoundException ex){
        log.warn(ex.getMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Пользователя с таким email не существует")
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccountNotEnabledEx.class)
    public ResponseEntity<ApiError> handleAccountNotEnabledEx(AccountNotEnabledEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.FORBIDDEN)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Qr2faGenerationEx.class)
    public ResponseEntity<ApiError> handleQr2faGenerationEx(Qr2faGenerationEx ex){
        log.error(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(WeakPasswordEx.class)
    public ResponseEntity<ApiError> handleWeaKPasswordEx(WeakPasswordEx ex){
        log.warn(ex.getDebugMessage());
        ApiError info = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.badRequest().body(info);
    }


    @ExceptionHandler(TfaNotEnabledEx.class)
    public ResponseEntity<ApiError> handleTfaNotEnabledEx(TfaNotEnabledEx ex){
        log.warn(ex.getDebugMessage());
        ApiError info = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.badRequest().body(info);
    }

    @ExceptionHandler(AuthenticationEx.class)
    public ResponseEntity<ApiError> handleAuthEx(AuthenticationEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.UNAUTHORIZED)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ResetTfaSecretTokenRenewEx.class)
    public ResponseEntity<ApiError> handleResetTfaTokenRenewEx(ResetTfaSecretTokenRenewEx ex){
        log.warn(ex.getDebugMessage());
        ApiError info = ApiError
                .builder()
                .status(HttpStatus.OK)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.ok().body(info);
    }


    @ExceptionHandler(TfaAlreadyEnabledEx.class)
    public ResponseEntity<ApiError> handleTfaAlreadyEnabledEx(TfaAlreadyEnabledEx ex){
        log.warn(ex.getDebugMessage());
        ApiError error = ApiError
                .builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
