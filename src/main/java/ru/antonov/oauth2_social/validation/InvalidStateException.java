package ru.antonov.oauth2_social.validation;

public class InvalidStateException extends RuntimeException{
    public InvalidStateException(String msg){
        super(msg);
    }
}
