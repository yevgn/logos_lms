package ru.antonov.oauth2_social.course.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.exception.FileNameNotUniqueEx;
import ru.antonov.oauth2_social.course.exception.TaskAndMaterialFileLimitExceededEx;
import ru.antonov.oauth2_social.exception.IOEx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileService {
    private final CourseLimitCounter courseLimitCounter;
    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    public void uploadCourseMaterialContentFiles(List<FileInfo> files) {
        for (FileInfo fileInfo : files) {
            MultipartFile file = fileInfo.getFile();
            if (!file.isEmpty()) {
                try {
                    Path path = Paths.get(fileInfo.getPath());
                    Files.createDirectories(path.getParent());
                    file.transferTo(path.toFile());
                } catch (IOException e) {
                    throw new IOEx(
                            "Ошибка на сервере",
                            "Ошибка при записи файлов в uploadCourseMaterialContentFiles"
                    );
                }
            }
        }
    }

    public void deleteDirectory(String dirPath) {
        Path dir = Paths.get(dirPath);
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ex) {
                                throw new IOEx(
                                        "Ошибка на сервере",
                                        "Ошибка при удалении каталога в deleteDirectory"
                                );
                            }
                        });
            } catch (IOException ex) {
                throw new IOEx(
                        "Ошибка на сервере",
                        "Ошибка при удалении каталога в deleteDirectory"
                );
            }
        } else{
            throw new IllegalArgumentException("Ошибка при удалении директории. Директории не существует, либо это" +
                    " не является директорией");
        }
    }

    public void deleteFile(String filePath){
        Path path = Paths.get(filePath);
        if(Files.exists(path) && !Files.isDirectory(path)){
            try {
                Files.delete(path);
            } catch (IOException ex){
                throw new IOEx(
                        "Ошибка на сервере",
                        "Ошибка при удалении файла в deleteFile"
                );
            }
        } else{
            throw new IllegalArgumentException("Ошибка при удалении файла");
        }
    }

    private boolean isFileAlreadyExist(FileInfo fileInfo) {
        // todo СДЕЛАТЬ
        return false;
    }

    public String generateCourseMaterialContentFilePath(UUID courseId, UUID courseMaterialId, UUID fileId, String extension) {
        return basePath + "/courses/" + courseId + "/course_materials/" + courseMaterialId + "/" + fileId + "." + extension;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class FileInfo {
        private String path;
        private String fileName;
        private MultipartFile file;
        private UUID id;
    }
}
