package ru.antonov.oauth2_social.auth.controller;

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

    @PostMapping("/make-auth")
    public ResponseEntity<AuthResponseDto> authenticate(@Valid @RequestBody AuthRequestDto request){
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/verify-tfa-code")
    public ResponseEntity<AuthResponseDto> verifyTfaCode(@Valid @RequestBody VerificationRequestDto request){
        return ResponseEntity.ok(authService.verifyCode(request));
    }

    @PostMapping("/refresh-access-token")
    public ResponseEntity<AuthResponseDto> refreshAccessToken(@Valid @RequestBody RefreshAccessTokenRequestDto request){
        return ResponseEntity.ok(authService.refreshAccessToken(request));
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal User user){
        authService.logout(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/send-mail-for-password-reset")
    public ResponseEntity<?> sendMailForPasswordReset(
            @RequestParam
            @Pattern(
                    regexp = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                    message = "Неправильный формат email"
            )
            String email
    )
    {
        authService.sendMailForPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request)
    {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/send-mail-for-tfa-secret-reset")
    public ResponseEntity<?> sendMailForTfaSecretReset(@Valid @RequestBody AuthRequestDto request)
    {
        authService.sendMailForTfaSecretReset(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reset-tfa")
    public ResponseEntity<AuthResponseDto> resetTfa(
            @RequestParam("user_email") String email,
            @RequestParam("reset-fta-token") String token
    )
    {
        return ResponseEntity.ok(authService.resetTfa(email, token));
    }

    @GetMapping("/enable-tfa")
    public ResponseEntity<AuthResponseDto> enableTfa(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AuthRequestDto request)
    {
        return ResponseEntity.ok(authService.enable2fa(user, request));
    }

    @GetMapping("/disable-tfa")
    public ResponseEntity<AuthResponseDto> disableTfa(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AuthRequestDto request,
            @RequestParam("tfa_code") String tfaCode)
    {
        return ResponseEntity.ok(authService.disableTfa(user, request, tfaCode));
    }

    @GetMapping("/confirm-reset-2fa-secret")
    public ResponseEntity<AuthResponseDto> confirm2faSecretReset(
            @RequestParam("user_email") String email, @RequestParam("token") String token)
    {
        authService.resetTfa(email, token);
        return ResponseEntity.ok().build();
    }
}
