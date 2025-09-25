package ru.antonov.oauth2_social.controller;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ru.antonov.oauth2_social.dto.TokenRequestDto;
import ru.antonov.oauth2_social.entity.TokenMode;
import ru.antonov.oauth2_social.entity.TokenType;
import ru.antonov.oauth2_social.entity.UserEntity;
import ru.antonov.oauth2_social.service.TokenService;
import ru.antonov.oauth2_social.service.UserService;

import ru.antonov.oauth2_social.validation.SecuredEndpointAccessEx;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/token")
@Slf4j
public class TokenController {
    private final TokenService tokenService;
    private final UserService userService;

    @Value("${application.jwt.inner-services-token}")
    private String innerServiceToken;

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@Valid @RequestBody TokenRequestDto tokenRequestDto){
        log.info("validateToken() in TokenController...");
        return ResponseEntity.ok(
                tokenService.isTokenValid(tokenRequestDto)
        );
    }

    @PostMapping("/refresh-access-token")
    public ResponseEntity<?> refreshAccessToken(@Valid @RequestBody TokenRequestDto tokenRequestDto){
        log.info("refreshAccessToken() in TokenController...");
        return ResponseEntity.ok(
                tokenService.refreshAccessToken(tokenRequestDto)
        );
    }

    @GetMapping("/email-confirmation-token/create")
    public ResponseEntity<?> createEmailConfirmationToken(
            @RequestParam("user_email")
            @Valid
            @Pattern(regexp = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                    message = "Неправильный формат email")
            String email,
            HttpServletRequest request){

        checkServiceToken(request);
        UserEntity user = userService.findUserByEmail(email);
        String verificationToken = tokenService.generateUserToken(List.of(), email, TokenMode.EMAIL_CONFIRMATION);
        tokenService.saveToken(verificationToken, TokenType.BEARER, TokenMode.EMAIL_CONFIRMATION, user);

        return ResponseEntity.ok(
                Map.of("confirmation_token", verificationToken)
        );
    }

    @GetMapping("/course-join-confirmation-token/create")
    public ResponseEntity<?> createCourseJoinConfirmationToken(
            @RequestParam("user_email")
            @Valid
            @Pattern(regexp = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                    message = "Неправильный формат email")
            String email,
            HttpServletRequest request){

        checkServiceToken(request);
        UserEntity user = userService.findUserByEmail(email);
        String verificationToken = tokenService.generateUserToken(List.of(), email, TokenMode.COURSE_JOIN_CONFIRMATION);
        tokenService.saveToken(verificationToken, TokenType.BEARER, TokenMode.COURSE_JOIN_CONFIRMATION, user);

        return ResponseEntity.ok(
                Map.of("confirmation_token", verificationToken)
        );
    }

    @GetMapping("/confirmation-token/username")
    public ResponseEntity<?> extractUsername( @RequestParam String token, HttpServletRequest request){
        checkServiceToken(request);
        if(tokenService.validateToken(token, TokenMode.EMAIL_CONFIRMATION)) {
            return ResponseEntity.ok(
                    Map.of("username", tokenService.extractUsername(token))
            );
        }
        throw new JwtException("Токен не прошел валидацию");
    }

    @GetMapping("/email-confirmation-token/revoke")
    public ResponseEntity<?> revokeUserEmailConfirmationTokens(
            @RequestParam("email") String email,
            HttpServletRequest request
    ){
        checkServiceToken(request);
        tokenService.revokeAllTokensByEmailAndTokenMode(email, TokenMode.EMAIL_CONFIRMATION);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/course-join-confirmation-token/revoke")
    public ResponseEntity<?> revokeUserCourseJoinConfirmationTokens(
            @RequestParam("email") String email,
            HttpServletRequest request
    ) {
        checkServiceToken(request);
        tokenService.revokeAllTokensByEmailAndTokenMode(email, TokenMode.COURSE_JOIN_CONFIRMATION);
        return ResponseEntity.ok().build();
    }

    private void checkServiceToken(HttpServletRequest request){
        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ") ){
            throw new SecuredEndpointAccessEx("Ключ отсутствует");
        }

        String token = authHeader.substring(7);
        if(!innerServiceToken.equals(token)){
            throw new SecuredEndpointAccessEx("Неправильный ключ");
        }
    }
}
