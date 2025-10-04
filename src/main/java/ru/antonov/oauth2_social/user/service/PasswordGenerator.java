package ru.antonov.oauth2_social.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.passay.CharacterRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordGenerator {
    public String generateRawPassword(int length, List<CharacterRule> rules){
        org.passay.PasswordGenerator gen = new org.passay.PasswordGenerator();
        return gen.generatePassword(length, rules);
    }
}
