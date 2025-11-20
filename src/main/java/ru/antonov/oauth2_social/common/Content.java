package ru.antonov.oauth2_social.common;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Content {
    private UUID id;
    private String originalFileName;
    private String path;
}
