package ru.antonov.oauth2_social.validation;

public class ClientNotFoundException extends RuntimeException{
    public ClientNotFoundException(String msg){
        super(msg);
    }
}
