package ru.antonov.oauth2_social.validation;

public class OAuth2AuthorizationException extends RuntimeException{
    public OAuth2AuthorizationException(String msg){
        super(msg);
    }
}
