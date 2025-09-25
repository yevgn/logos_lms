package ru.antonov.oauth2_social.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import org.springframework.web.bind.annotation.*;

import ru.antonov.oauth2_social.service.AuthService;

import java.io.IOException;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    private final String AUTH_REQUEST_TEMPLATE = "%s?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s";

    @GetMapping("/authenticate")
    public ResponseEntity<Object> authenticate(
            HttpServletRequest request, HttpServletResponse response,
            @NotEmpty(message = "Поле registration_id не может быть пустым")
            @RequestParam(name = "registration_id") String registrationId) throws IOException {

        ClientRegistration client = authService.findClientById(registrationId);

        String scope = authService.getScope(client);

        String state = UUID.randomUUID().toString();

        request.getSession().setAttribute("oauth2State", state);

        String authUrl = String.format(
                AUTH_REQUEST_TEMPLATE, client.getProviderDetails().getAuthorizationUri(), client.getClientId(),
                client.getRedirectUri(), scope, state
        );

        response.sendRedirect(authUrl);

        return ResponseEntity.status(HttpStatus.FOUND).build();
    }

    @GetMapping("/callback/yandex")
    public ResponseEntity<?> callbackFromYandex(@RequestParam("code") String code, @RequestParam("state") String state,
                                                HttpServletRequest request) {

        ClientRegistration client = authService.findClientById("yandex");
        return ResponseEntity.ok(
                authService.callback(code, state, client, request)
        );
    }

    @GetMapping("/callback/google")
    public ResponseEntity<?> callbackFromGoogle(@RequestParam("code") String code, @RequestParam("state") String state,
                                                HttpServletRequest request) {

        ClientRegistration client = authService.findClientById("google");
        return ResponseEntity.ok(
                authService.callback(code, state, client, request)
        );
    }
}
