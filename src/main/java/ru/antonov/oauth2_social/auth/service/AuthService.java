package ru.antonov.oauth2_social.auth.service;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;


import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import ru.antonov.oauth2_social.auth.dto.*;

import ru.antonov.oauth2_social.auth.entity.TokenMode;
import ru.antonov.oauth2_social.auth.entity.TokenType;
import ru.antonov.oauth2_social.auth.exception.*;
import ru.antonov.oauth2_social.common.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.auth.exception.AccountActivationTokenRenewEx;
import ru.antonov.oauth2_social.user.service.UserService;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final UserService userService;
    private final TwoFactorAuthenticationService tfaAuthService;
    private final AuthEmailService authEmailService;
    private final PasswordValidationService passwordValidationService;
    //private final PasswordEncoder passwordEncoder;

    public AuthResponseDto authenticate(AuthRequestDto request) {
        tryAuth(request.getEmail(), request.getPassword());

        var user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь с таким email не найден",
                        String.format("Неудачная аутентификация. Пользователь с email %s не найден", request.getEmail())
                ));

        if (user.isTfaEnabled()) {
            return AuthResponseDto.builder()
                    .accessToken("")
                    .refreshToken("")
                    .isTfaEnabled(true)
                    .build();
        }

        tokenService.revokeUserTokensByTokenModeIn(user.getEmail(), List.of(TokenMode.ACCESS, TokenMode.REFRESH));

        var accessToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.ACCESS);
        var refreshToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.REFRESH);

        tokenService.saveToken(accessToken, TokenType.BEARER, TokenMode.ACCESS, user);
        tokenService.saveToken(refreshToken, TokenType.BEARER, TokenMode.REFRESH, user);

        return AuthResponseDto.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isTfaEnabled(false)
                .build();
    }

    public AuthResponseDto verifyCode(VerificationRequestDto request) {
        User user = userService
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь с таким email не найден",
                        String.format("Ошибка при проверке 2FA code. Пользователь с email %s не найден",
                                request.getEmail()))
                );

        if (tfaAuthService.isOtpNotValid(user.getTfaSecret(), request.getCode())) {
            throw new BadTfaCodeEx(
                    "Неправильный код",
                    String.format("Ошибка верификации кода 2FA. Неправильный код 2FA пользователя %s", user.getId())
            );
        }

        tokenService.revokeUserTokensByTokenModeIn(user.getEmail(), List.of(TokenMode.ACCESS, TokenMode.REFRESH));

        var accessToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.ACCESS);
        var refreshToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.REFRESH);

        tokenService.saveToken(accessToken, TokenType.BEARER, TokenMode.ACCESS, user);
        tokenService.saveToken(refreshToken, TokenType.BEARER, TokenMode.REFRESH, user);

        return AuthResponseDto.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isTfaEnabled(user.isTfaEnabled())
                .build();
    }

    public AuthResponseDto refreshAccessToken(RefreshAccessTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtService.isTokenValid(refreshToken, TokenMode.REFRESH)) {
            throw new JwtEx(
                    "Refresh токен истек или неправильно сконфигурирован",
                    String.format("Ошибка при обновлении токена доступа. refresh Токен не прошел валидацию: %s",
                            refreshToken
                    )
            );
        }

        User user = tokenService.findUserByToken(refreshToken)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Владелец токена не найден",
                        String.format("Ошибка при обновлении токена доступа. " +
                                "Пользователь по токену %s не найден", refreshToken
                        )
                ));

        tokenService.revokeUserTokensByTokenModeIn(user.getEmail(), List.of(TokenMode.ACCESS, TokenMode.REFRESH));

        String newAccessToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.ACCESS);

        tokenService.saveToken(newAccessToken, TokenType.BEARER, TokenMode.ACCESS, user);

        return AuthResponseDto
                .builder()
                .userId(user.getId())
                .accessToken(newAccessToken)
                .refreshToken("")
                .isTfaEnabled(user.isTfaEnabled())
                .build();
    }

    public void logout(User user) {
        tokenService.revokeUserTokensByTokenModeIn(user.getEmail(), List.of(TokenMode.ACCESS, TokenMode.REFRESH));
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendMailForPasswordReset(String email) {
        User user = userService
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь с таким email не найден",
                        String.format("Ошибка отправки письма для сброса пароля. " +
                                "Пользователь с email %s не найден", email
                        )
                ));

//        if(!user.isEnabled()){
//            throw new AccountNotEnabledEx(
//                    String.format("Аккаунт пользователя %s не активирован", email)
//            );
//        }

        tokenService.revokeUserTokensByTokenModeIn(email, List.of(TokenMode.RESET_PASSWORD));

        String resetPasswordToken = jwtService.generateUserToken(List.of(user.getRole().name()), email, TokenMode.RESET_PASSWORD);
        tokenService.saveToken(resetPasswordToken, TokenType.BEARER, TokenMode.RESET_PASSWORD, user );

        authEmailService.sendMailForPasswordReset(user, resetPasswordToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequestDto request) {
        String email = request.getEmail();
        String token = request.getResetPasswordToken();

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Пользователь не найден",
                        String.format("Ошибка сброса пароля. Пользователь с %s email не найден", email)
                ));

        User tokenUser = tokenService.findUserByToken(token)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Пользователь не найден",
                        String.format("Ошибка сброса пароля. Пользователь по токену %s не найден", token)
                ));

        if (!tokenUser.getId().equals(user.getId())) {
            throw new TokenUserMismatchEx(
                    "Данный токен принадлежит другому пользователю.",
                    String.format("Ошибка сброса пароля. Несовпадение: токен принадлежит пользователю %s." +
                                    " Заявленный пользователь: %s",
                            tokenUser.getId(), user.getId()
                    )
            );
        }

        if (!jwtService.isTokenValid(token, TokenMode.RESET_PASSWORD)) {
            sendMailForPasswordReset(email);
            throw new ResetPasswordTokenRenewEx(
                    "Время действия ссылки истекло. Вы получите новую ссылку на ваш email",
                    String.format("Неуспешный сброс пароля. reset_password_token пользователя %s истек. " +
                            "Произошла выдача нового токена", user.getId())
            );
        }

        String newPassword = request.getNewPassword();
        if(!passwordValidationService.isValid(newPassword)){
            throw new WeakPasswordEx(
                    "Слишком слабый пароль. Пароль должен содержать от 8 до 64 символов, содержать хотя бы одну" +
                            " прописную английскую букву, одну строчную английскую букву, одну цифру и один специальный" +
                            " символ.",
                    String.format("Ошибка сброса пароля. Новый пароль %s пользователя %s не прошел валидацию",
                            newPassword, user.getId())
            );
        }

        user.setPassword(newPassword);
        userService.save(user);

        tokenService.revokeUserTokensByTokenModeIn(
                email,
                List.of(TokenMode.RESET_PASSWORD, TokenMode.ACCESS, TokenMode.REFRESH)
        );

        authEmailService.sendPasswordSuccessfulResetNotification(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendMailForTfaSecretReset(AuthRequestDto request) {
        String email = request.getEmail();

        tryAuth(email, request.getPassword());

        var user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Ошибка. Пользователь с таким email не найден",
                        String.format("Ошибка сброса 2FA secret. Пользователь с email %s не найден", email))
                );

        if(!user.isTfaEnabled()){
            throw new TfaNotEnabledEx(
                    "На этом аккаунте не включена двухфакторная аутентификация",
                    String.format("Ошибка сброса 2FA secret. У пользователя %s не включена 2FA", user.getId())
            );
        }

        sendMailForTfaSecretResetPriv(email);
    }

    private void sendMailForTfaSecretResetPriv(String email) {
        var user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь с таким email не найден",
                        String.format("Ошибка сброса 2FA secret. Пользователь с email %s не найден", email))
                );

        tokenService.revokeUserTokensByTokenModeIn(email, List.of(TokenMode.RESET_2FA));

        String resetTfaToken = jwtService.generateUserToken(List.of(user.getRole().name()), email, TokenMode.RESET_2FA);
        tokenService.saveToken(resetTfaToken, TokenType.BEARER, TokenMode.RESET_2FA, user );

        authEmailService.sendMailForTfaReset(user, resetTfaToken);
    }

    private void tryAuth(String email, String password){
        try{
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                           email,
                          password
                    )
            );
        } catch (DisabledException ex){
            throw new AuthenticationEx(
                    "Ошибка аутентификации. Ваш аккаунт не активирован. " +
                            "Чтобы активировать аккаунт, подтвердите ваш email",
                    String.format("Ошибка аутентификации. Аккаунт пользователя %s не активирован", email)
            );
        } catch (AuthenticationException ex){
            throw new AuthenticationEx(
                    "Введены неправильные данные",
                    String.format("Неуспешная аутентификация. Неуспешная аутентификация пользователя %s", email)
            );
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetTfa(String email, String token) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь с таким email не найден",
                        String.format("Ошибка сброса 2FA secret. Пользователь с %s email не найден", email)
                ));

        User tokenUser = tokenService.findUserByToken(token)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь не найден",
                        String.format("Ошибка сброса 2FA secret. Пользователь по токену %s не найден", token)
                ));

        if (!tokenUser.equals(user)) {
            throw new TokenUserMismatchEx(
                    "Токен принадлежит другому пользователю",
                    String.format("Ошибка сброса 2FA secret. Несовпадение: токен принадлежит пользователю %s. " +
                                    "Заявленный пользователь: %s",
                            tokenUser.getId(), user.getId()
                    )
            );
        }

        if (!jwtService.isTokenValid(token, TokenMode.RESET_2FA)) {
            sendMailForTfaSecretResetPriv(email);
            throw new ResetTfaSecretTokenRenewEx(
                    "Срок действия ссылки истек. Новая ссылка была отправлена на ваш email.",
                    String.format("Ошибка сброса 2FA secret. RESET_2FA_TOKEN пользователя %s истек. " +
                            "Произошла выдача нового токена", email)
            );
        }

        user.setTfaSecret(null);
        user.setTfaEnabled(false);
        userService.save(user);

        tokenService.revokeUserTokensByTokenModeIn(
                user.getEmail(),
                List.of(TokenMode.ACCESS, TokenMode.REFRESH, TokenMode.RESET_2FA)
        );

        authEmailService.sendTfaSuccessfulResetNotification(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthResponseDto enable2fa(User user, AuthRequestDto request) {
        tryAuth(request.getEmail(), request.getPassword());

        if(!isCorrectCredentials(user, request.getEmail(), request.getPassword())){
            throw new AuthenticationEx(
                    "Введены неправильные данные",
                    String.format("Неуспешная аутентификация. Неуспешная аутентификация пользователя %s", user.getId())
            );
        }

        if (user.isTfaEnabled()) {
            throw new TfaAlreadyEnabledEx(
                    "На этом аккаунте уже включена двухфакторная аутентификация",
                    String.format("Ошибка подключения 2FA. 2FA для пользователя %s уже включена", user.getId())
            );
        }

        user.setTfaSecret(tfaAuthService.generateNewSecret());
        user.setTfaEnabled(true);
        userService.save(user);

        tokenService.revokeUserTokensByTokenModeIn(user.getEmail(), List.of(TokenMode.ACCESS, TokenMode.REFRESH));
        String accessToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.ACCESS);
        String refreshToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.REFRESH);

        tokenService.saveToken(accessToken, TokenType.BEARER, TokenMode.ACCESS, user);
        tokenService.saveToken(refreshToken, TokenType.BEARER, TokenMode.REFRESH, user);

        authEmailService.sendTfaSuccessfulEnabledNotification(user);

        return AuthResponseDto
                .builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isTfaEnabled(user.isTfaEnabled())
                .secretImageUri(tfaAuthService.generateQrCodeImageUri(user.getTfaSecret(), user.getEmail()))
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthResponseDto disableTfa(User user, AuthRequestDto request, String tfaCode) {
        String email = request.getEmail();
        String password = request.getPassword();

        tryAuth(email, password);

        if(!isCorrectCredentials(user, email, password)){
            throw new AuthenticationEx(
                    "Введены неправильные данные",
                    String.format("Неуспешная аутентификация. Неуспешная аутентификация пользователя %s", user.getId())
            );
        }

        if(tfaAuthService.isOtpNotValid(user.getTfaSecret(), tfaCode)){
            throw new BadTfaCodeEx(
                    "Неправильный код",
                    String.format("Ошибка отключения 2FA. Неправильный код 2FA пользователя %s", user.getId())
            );
        }

        user.setTfaSecret(null);
        user.setTfaEnabled(false);
        userService.save(user);

        tokenService.revokeUserTokensByTokenModeIn(user.getEmail(), List.of(TokenMode.ACCESS, TokenMode.REFRESH));
        String accessToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.ACCESS);
        String refreshToken = jwtService.generateUserToken(List.of(user.getRole().name()), user.getEmail(), TokenMode.REFRESH);

        tokenService.saveToken(accessToken, TokenType.BEARER, TokenMode.ACCESS, user);
        tokenService.saveToken(refreshToken, TokenType.BEARER, TokenMode.REFRESH, user);

        authEmailService.sendTfaSuccessfulDisabledNotification(user);

        return AuthResponseDto
                .builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isTfaEnabled(user.isTfaEnabled())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void activateAccount(String accountActivationToken, String userEmail){
        User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь не найден",
                        String.format("Ошибка активации аккаунта. Пользователь с %s email не найден", userEmail)
                ));

        if(user.isEnabled()){
            throw new AccountAlreadyEnabledEx(
                    "Ваш аккаунт уже активирован!",
                    String.format("Ошибка при активации аккаунта. Аккаунт пользователя %s уже активирован!", user.getId())
            );
        }

        User tokenUser = tokenService.findUserByToken(accountActivationToken)
                .orElseThrow(() -> new EntityNotFoundEx(
                        "Пользователь не найден",
                        String.format("Ошибка активации аккаунта. Пользователь по токену %s не найден", accountActivationToken)
                ));

        if (!tokenUser.equals(user)) {
            throw new TokenUserMismatchEx(
                    "Данный токен принадлежит другому пользователю.",
                    String.format("Ошибка активации аккаунта. Несовпадение: токен принадлежит пользователю %s." +
                                    " Заявленный пользователь: %s",
                            tokenUser.getId(), user.getId()
                    )
            );
        }

        if (!jwtService.isTokenValid(accountActivationToken, TokenMode.ACCOUNT_ACTIVATION)) {
            tokenService.revokeUserTokensByTokenModeIn(userEmail, List.of(TokenMode.ACCOUNT_ACTIVATION));
            String newToken = jwtService.generateUserToken(
                    List.of(user.getRole().name()), userEmail, TokenMode.ACCOUNT_ACTIVATION
            );
            tokenService.saveToken(newToken, TokenType.BEARER, TokenMode.ACCOUNT_ACTIVATION, user);
            authEmailService.sendMailForAccountActivation(user, newToken);
            throw new AccountActivationTokenRenewEx(
                    "Время действия ссылки истекло. Вы получите новую ссылку на ваш email",
                    String.format("Неуспешная активация аккаунта. account_activation_token пользователя %s истек. " +
                            "Произошла выдача нового токена", user.getId())
            );
        }

        userService.enableAndSave(user);

        tokenService.revokeUserTokensByTokenModeIn(userEmail, List.of(TokenMode.ACCOUNT_ACTIVATION));
    }

    private boolean isCorrectCredentials(User user, String email, String password){
        return Objects.equals(user.getEmail(), email) && Objects.equals(user.getPassword(), password);
    }
}
