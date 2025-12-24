package ru.antonov.oauth2_social.auth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountNotEnabledEx extends RuntimeException{
    private String debugMessage;

    public AccountNotEnabledEx(String msg, String debugMessage){
        super(msg);
        this.debugMessage = debugMessage;
    }
}
