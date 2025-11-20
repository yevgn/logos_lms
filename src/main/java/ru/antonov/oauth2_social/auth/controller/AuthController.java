package ru.antonov.oauth2_social.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.antonov.oauth2_social.auth.dto.*;

import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.auth.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Аутентификация пользователя")
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
            @ApiResponse(responseCode = "401",
                    description = "Неуспешная аутентификация (введены неверные данные или аккаунт не активирован)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "401",
                                          "message": "Ошибка аутентификации. Введеные неправильные данные"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректный запрос (например, неверные данные или данные отсутствуют)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Поле email не может быть пустым"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/make-auth")
    public ResponseEntity<AuthResponseDto> authenticate(@Valid @RequestBody AuthRequestDto request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @Operation(summary = "Верификация 2FA кода")
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная верификация"),
            @ApiResponse(responseCode = "401",
                    description = "Неуспешная верификация 2FA кода",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "401",
                                          "message": "Неправильный код"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "400",
                    description = "Некорректный запрос (например, неверные данные или данные отсутствуют)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Поле code не может быть пустым"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/verify-tfa-code")
    public ResponseEntity<AuthResponseDto> verifyTfaCode(@Valid @RequestBody VerificationRequestDto request) {
        return ResponseEntity.ok(authService.verifyCode(request));
    }

    @Operation(
            summary = "Обновление токена доступа",
            description = "Токен доступа можно обновить по refresh токену"
    )
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное обновление токена доступа"),
            @ApiResponse(responseCode = "400",
                    description = "Некорректный запрос (например, неверный токен или токен отсутствует)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Refresh токен истек или неправильно сконфигурирован"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/refresh-access-token")
    public ResponseEntity<AuthResponseDto> refreshAccessToken(@Valid @RequestBody RefreshAccessTokenRequestDto request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request));
    }

    @Operation(
            summary = "Завершение сессии",
            description = "Инвалидация access и refresh токенов пользователя",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное завершение сессии"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Учетная запись не активирована (не подтверждена эл. почта)",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Ваш аккаунт не активирован. Чтобы активировать аккаунт, подтвердите ваш email"
                                        }
                                    """)
            ))
    }
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal User user) {
        authService.logout(user);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Отправка сообщения на эл. почту пользователя для сброса пароля")
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сообщение успешно отправлено"),
            @ApiResponse(responseCode = "400",
                    description = "Некорректный запрос (например, неверные данные или данные отсутствуют)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Пользователь с email test@gmail.com не найден"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере (например, не удалось отправить письмо на SMTP-сервер)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/send-mail-for-password-reset")
    public ResponseEntity<?> sendMailForPasswordReset(
            @RequestParam
            @Pattern(
                    regexp = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                    message = "Неправильный формат email"
            )
            String email
    ) {
        authService.sendMailForPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Сброс пароля")
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пароль успешно сброшен или отправлена новая ссылка на эл. почту",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                                {
                                                  "status" : "200",
                                                  "message": "Время действия ссылки истекло. Вы получите новую ссылку на ваш email"
                                                }
                                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос (например, неверные данные или данные отсутствуют)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                                {
                                                  "status" : "400",
                                                  "message": "Поле email не должно быть пустым"
                                                }
                                            """)
                    )
            ),

    })
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @Operation(summary = "Отправка сообщения на эл. почту пользователя для сброса 2FA secret")
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сообщение успешно отправлено"),
            @ApiResponse(responseCode = "400",
                    description = "Некорректный запрос (например, неверные данные или данные отсутствуют)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                                {
                                                  "status" : "400",
                                                  "message": "На этом аккаунте не включена двухфакторная аутентификация"
                                                }
                                            """)
                            }
                    )),
            @ApiResponse(responseCode = "401",
                    description = "Введены неправильные эл. почта или пароль",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "401",
                                          "message": "Введены неправильные данные"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере (например, не удалось отправить письмо на SMTP-сервер)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/send-mail-for-tfa-secret-reset")
    public ResponseEntity<?> sendMailForTfaSecretReset(@Valid @RequestBody AuthRequestDto request) {
        authService.sendMailForTfaSecretReset(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Сброс 2FA secret")
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "2FA secret успешно сброшен или отправлена новая ссылка на эл. почту",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                       "status" : "200",
                                       "message": "Время действия ссылки истекло. Вы получите новую ссылку на ваш email"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400",
                    description = "Некорректный запрос (например, неверные данные или данные отсутствуют)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                                {
                                                  "status" : "400",
                                                  "message": "Пользователь с таким email не найден"
                                                }
                                            """)
                            }
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере (не удалось отправить письмо с уведомлением о сбросе 2FA secret на SMTP-сервер)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/reset-tfa")
    public ResponseEntity<AuthResponseDto> resetTfa(
            @RequestParam("user_email") String email,
            @RequestParam("reset-fta-token") String token
    ) {
        return ResponseEntity.ok(authService.resetTfa(email, token));
    }


    @Operation(summary = "Включение двухфакторной аутентификации", security = @SecurityRequirement(name = "bearerAuth"))
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Двухфакторная аутентификация успешно включена"
            ),
            @ApiResponse(responseCode = "400",
                    description = "Некорректный запрос (например, неверные данные или данные отсутствуют)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                                {
                                                  "status" : "400",
                                                  "message": "На этом аккаунте уже включена двухфакторная аутентификация"
                                                }
                                            """)
                            }
                    )),
            @ApiResponse(responseCode = "401",
                    description = "Пользователь не аутентифицирован",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "401",
                                          "message": "Введены неправильные данные"
                                        }
                                    """)
                    )),
            @ApiResponse(
                    responseCode = "403",
                    description = "Учетная запись не активирована (не подтверждена эл. почта)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Ваш аккаунт не активирован. Чтобы активировать аккаунт, подтвердите ваш email"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере (не удалось отправить письмо с уведомлением о включении 2FA на SMTP-сервер)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/enable-tfa")
    public ResponseEntity<AuthResponseDto> enableTfa(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AuthRequestDto request) {
        return ResponseEntity.ok(authService.enable2fa(user, request));
    }

    @Operation(summary = "Отключение двухфакторной аутентификации", security = @SecurityRequirement(name = "bearerAuth"))
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Двухфакторная аутентификация успешно отключена"
            ),
            @ApiResponse(responseCode = "400",
                    description = "Некорректный запрос (например, неверные данные или данные отсутствуют)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                                {
                                                  "status" : "400",
                                                  "message": "На этом аккаунте уже включена двухфакторная аутентификация"
                                                }
                                            """)
                            }
                    )),
            @ApiResponse(responseCode = "401",
                    description = "Пользователь не аутентифицирован",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = """
                                                {
                                                  "status" : "401",
                                                  "message": "Введены неправильные данные"
                                                }
                                            """)
                                    ,
                                    @ExampleObject(value = """
                                                {
                                                  "status" : "401",
                                                  "message": "Неправильный 2FA код"
                                                }
                                            """)
                            }
                    )),
            @ApiResponse(
                    responseCode = "403",
                    description = "Учетная запись не активирована (не подтверждена эл. почта)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "403",
                                          "message": "Ошибка доступа. Ваш аккаунт не активирован. Чтобы активировать аккаунт, подтвердите ваш email"
                                        }
                                    """)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Ошибка на сервере (не удалось отправить письмо с уведомлением об отключении 2FA на SMTP-сервер)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "500",
                                          "message": "Ошибка на сервере"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/disable-tfa")
    public ResponseEntity<AuthResponseDto> disableTfa(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AuthRequestDto request,
            @RequestParam("tfa_code") String tfaCode) {
        return ResponseEntity.ok(authService.disableTfa(user, request, tfaCode));
    }

    @Operation(summary = "Активация аккаунта")
    @Tag(name = "Аутентификация. Управление токенами, сессиями")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Аккаунт успешно активирован или новая ссылка для активации отправлена на эл. почту",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                                {
                                                  "status" : "200",
                                                  "message": "Время действия ссылки истекло. Вы получите новую ссылку на ваш email"
                                                }
                                            """)
                    )
            ),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные  данные или они отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                        {
                                          "status" : "400",
                                          "message": "Ваш аккаунт уже активирован!"
                                        }
                                    """)
                    ))
    }
    )
    @PostMapping("/activate-account")
    public ResponseEntity<String> activateAccount(
            @RequestParam("account_activation_token") String accountActivationToken,
            @RequestParam("user_email") String userEmail
    ){

        authService.activateAccount(accountActivationToken, userEmail);
        return ResponseEntity.ok("Вы успешно активировали свой аккаунт!");
    }
}
