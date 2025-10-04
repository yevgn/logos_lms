package ru.antonov.oauth2_social.config;


import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
@Builder
public class ApiErrorList {
    private HttpStatus status;
    private String message;
    private List<String> errors;
}