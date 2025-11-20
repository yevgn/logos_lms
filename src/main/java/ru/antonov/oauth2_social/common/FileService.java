package ru.antonov.oauth2_social.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.antonov.oauth2_social.course.service.CourseLimitCounter;
import ru.antonov.oauth2_social.exception.IOEx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileService {
    private final CourseLimitCounter courseLimitCounter;
    @Value("${spring.application.file-storage.base-path}")
    private Path basePath;

    public void uploadFiles(List<FileInfo> files) {
        for (FileInfo fileInfo : files) {
            MultipartFile file = fileInfo.getFile();
            if (!file.isEmpty()) {
                try {
                    Path path = basePath.resolve(fileInfo.getPath());
                    Files.createDirectories(path.getParent());
                    file.transferTo(path.toFile());
                } catch (IOException e) {
                    throw new IOEx(
                            "Ошибка на сервере",
                            "Ошибка при записи файлов на диск"
                    );
                }
            }
        }
    }

    public String getFileExtension(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Ошибка получения расширения файла. filename = null");
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new IllegalArgumentException("Ошибка получения расширения файла. filename не содержит расширение");
        }

        return filename.substring(dotIndex + 1);
    }

    public void deleteDirectory(Path dirPath) {
        Path path = basePath.resolve(dirPath);
        if (Files.exists(path) && Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
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
        } else {
            throw new IllegalArgumentException("Ошибка при удалении директории. Директории не существует, либо это" +
                    " не является директорией");
        }
    }

    public void deleteFile(Path filePath) {
        Path path = basePath.resolve(filePath);
        if (Files.exists(path) && !Files.isDirectory(path)) {
            try {
                Files.delete(path);
            } catch (IOException ex) {
                throw new IOEx(
                        "Ошибка на сервере",
                        "Ошибка при удалении файла в deleteFile"
                );
            }
        } else {
            throw new IllegalArgumentException("Ошибка при удалении файла");
        }
    }

    private boolean isFileAlreadyExist(FileInfo fileInfo) {
        // todo СДЕЛАТЬ
        return false;
    }

    // относительный путь
    public Path generateCourseMaterialContentFilePath(UUID courseId, UUID courseMaterialId, UUID fileId, String extension) {
        return Path.of(
                "courses",
                courseId.toString(),
                "course_materials",
                courseMaterialId.toString(),
                fileId.toString() + "." + extension
        );
    }

    public Path generateTaskFilePath(UUID courseId, UUID taskId, UUID fileId, String extension) {
        return Path.of(
                "courses",
                courseId.toString(),
                "tasks",
                taskId.toString(),
                fileId.toString() + "." + extension
        );
    }

    public Path generateSolutionFilePath(UUID courseId, UUID taskId, UUID solutionId, UUID fileId, String extension){
        return Path.of(
                "courses",
                courseId.toString(),
                "tasks",
                taskId.toString(),
                "solutions",
                solutionId.toString(),
                fileId.toString() + "." + extension
        );
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class FileInfo {
        private Path path;
        private String fileName;
        private MultipartFile file;
        private UUID id;
    }
}
