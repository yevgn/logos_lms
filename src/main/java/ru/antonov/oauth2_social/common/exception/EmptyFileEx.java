package ru.antonov.oauth2_social.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmptyFileEx extends RuntimeException{
    private String debugMessage;

    public EmptyFileEx(String message, String debugMessage){
        super(message);
    }
}
