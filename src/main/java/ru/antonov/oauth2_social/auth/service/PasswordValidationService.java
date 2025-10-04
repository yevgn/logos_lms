package ru.antonov.oauth2_social.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.passay.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PasswordValidationService {
    public boolean isValid(String password){
        PasswordValidator validator = new PasswordValidator(
                new LengthRule(8, 64),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1),
                new WhitespaceRule() // запрещаем пробелы
        );

       return validator
               .validate(new PasswordData(password))
               .isValid();
    }
}
