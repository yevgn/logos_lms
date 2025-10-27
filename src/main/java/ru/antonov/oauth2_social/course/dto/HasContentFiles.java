package ru.antonov.oauth2_social.course.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HasContentFiles {
    List<MultipartFile> getContent();
}
