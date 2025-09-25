package ru.antonov.oauth2_social.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.antonov.oauth2_social.dto.AuthResponseDto;

import ru.antonov.oauth2_social.entity.TokenMode;
import ru.antonov.oauth2_social.entity.TokenType;
import ru.antonov.oauth2_social.entity.UserEntity;
import ru.antonov.oauth2_social.validation.AccountInactiveEx;
import ru.antonov.oauth2_social.validation.ClientNotFoundException;
import ru.antonov.oauth2_social.validation.InvalidStateException;
import ru.antonov.oauth2_social.validation.OAuth2AuthorizationException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final TokenService jwtService;
    private final UserService userService;
    private final TokenService tokenService;

    public ClientRegistration findClientById(String registrationId) {
        registrationId = registrationId.toLowerCase();

        ClientRegistration client = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (client == null) {
            throw new ClientNotFoundException(String.format("Способа входа %s не существует", registrationId));
        }
        return client;
    }

    public String getScope(ClientRegistration client) {
        return client.getScopes()
                .stream()
                .map(s -> s.trim() + " ")
                .reduce(String::concat)
                .orElseThrow()
                .trim();
    }

    public AuthResponseDto makeAuth(String code, ClientRegistration client) {
        // Обмен authorization_code, полученного на внешнем сервисе аутентификации, на access_token
        String token = getOauth2AccessToken(code, client);
        // Получение email'а пользователя по access token'у
        String userEmail = getUserEmail(
                client.getRegistrationId(), client.getProviderDetails().getUserInfoEndpoint().getUri(), token
        );

        UserEntity user = userService.findUserByEmail(userEmail);
        if(!user.isEnabled()){
            String msg = String.format(
                    "Аккаунт с %s email не активирован. Необходимо подтвердить адрес электронной почты",
                    user.getEmail()
            );
            log.error("Авторизация не пройдена: {}", msg);
            throw new AccountInactiveEx(msg);
        }

        // Генерация access и refresh token'ов
        String accessToken = jwtService.generateUserToken(List.of(user.getRole()), user.getEmail(), TokenMode.ACCESS);
        String refreshToken = jwtService.generateUserToken(List.of(user.getRole()), user.getEmail(), TokenMode.REFRESH);

        // Ранее выданные токены теперь не действительны
        int amount_revoked_tokens = tokenService.revokeAllUserTokens(user.getEmail());

        // сохранение токенов в БД
        tokenService.saveToken(accessToken, TokenType.BEARER, TokenMode.ACCESS, user);
        tokenService.saveToken(refreshToken, TokenType.BEARER, TokenMode.REFRESH, user);

        log.error("Авторизация пройдена успешно: {}", userEmail);
        return AuthResponseDto
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String getOauth2AccessToken(String code, ClientRegistration client) {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

        String authGrantType = client.getAuthorizationGrantType().getValue();
        String clientId = client.getClientId();
        String clientSecret = client.getClientSecret();
        String redirectUri = client.getRedirectUri();

        requestParams.add("grant_type", authGrantType);
        requestParams.add("client_id", clientId);
        requestParams.add("client_secret", clientSecret);
        requestParams.add("code", code);
        requestParams.add("redirect_uri", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestParams, headers);

        String tokenUri = client.getProviderDetails().getTokenUri();

        ResponseEntity<Map> responseEntity = restTemplate.exchange(tokenUri, HttpMethod.POST, requestEntity, Map.class);
        Map<String, Object> responseBody = responseEntity.getBody();

        if (responseBody != null && responseBody.containsKey("access_token")) {
            return (String) responseBody.get("access_token");
        }

        throw new OAuth2AuthorizationException(String.format("Ошибка при аутентификации через клиент %s", client));
    }

    private String getUserEmail(String registrationId, String userInfoUri, String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> responseEntity = restTemplate.exchange(userInfoUri, HttpMethod.GET, requestEntity, Map.class);
        Map<String, Object> responseBody = responseEntity.getBody();

        if (responseBody != null && registrationId.equals("google") && responseBody.get("email") != null) {
            return (String) responseBody.get("email");
        } else if (responseBody != null && registrationId.equals("yandex") && responseBody.get("default_email") != null) {
            return (String) responseBody.get("default_email");
        } else {
            throw new OAuth2AuthorizationException(String.format("Ошибка при авторизации с %s", registrationId));
        }
    }

    public AuthResponseDto callback(String code, String state, ClientRegistration client, HttpServletRequest request) {
        String savedState = (String) request.getSession().getAttribute("oauth2State");

        if (!Objects.equals(savedState, state)) {
            throw new InvalidStateException("Параметр запроса state был изменен!");
        }

        return makeAuth(code, client);
    }
}
