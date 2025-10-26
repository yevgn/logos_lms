package ru.antonov.oauth2_social.course.entity;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class CourseMaterialContent {
    private UUID id;
    private String originalFileName;
    private String path;
}
