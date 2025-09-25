package ru.antonov.oauth2_social.validation;

public class TokenConfigurationException extends RuntimeException{
    public TokenConfigurationException(String msg){
        super(msg);
    }
}
