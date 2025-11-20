package ru.antonov.oauth2_social.common;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HasContentFiles {
    List<MultipartFile> getContent();
}
