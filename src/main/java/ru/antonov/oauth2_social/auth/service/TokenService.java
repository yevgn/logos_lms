package ru.antonov.oauth2_social.auth.service;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;


import ru.antonov.oauth2_social.auth.entity.Token;
import ru.antonov.oauth2_social.auth.entity.TokenMode;
import ru.antonov.oauth2_social.auth.entity.TokenType;
import ru.antonov.oauth2_social.user.entity.User;
import ru.antonov.oauth2_social.auth.repository.TokenRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
    private final TokenRepository tokenRepository;

    public Optional<User> findUserByToken(String token){
        return tokenRepository.findUserByToken(token);
    }

    public Token saveToken(String token, TokenType tokenType, TokenMode tokenMode, User user) {
        Token tokenEntity = Token.builder()
                .token(token)
                .tokenMode(tokenMode)
                .tokenType(tokenType)
                .user(user)
                .build();
        return tokenRepository.saveAndFlush(tokenEntity);
    }

    public int revokeUserTokensByTokenModeIn(String email, List<TokenMode> modes){
        return tokenRepository.revokeAllUsersTokensByTokenModeIn(email, modes);
    }

    List<Token> findByUserEmailNotRevokedAndExpired(String userEmail){
        return tokenRepository.findByUserEmailNotRevokedAndNotExpired(userEmail);
    }

//    public int revokeAllUserTokens(String email){
//        return tokenRepository.revokeAllByUserEmail(email);
//    }
}