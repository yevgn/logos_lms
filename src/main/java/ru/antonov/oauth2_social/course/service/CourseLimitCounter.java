package ru.antonov.oauth2_social.course.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.antonov.oauth2_social.course.repository.CourseUserRepository;
import ru.antonov.oauth2_social.exception.IOEx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class CourseLimitCounter {
    @Value("${spring.application.course-limit-params.max-task-and-material-file-amount-for-course}")
    private int maxTaskAndMaterialFileAmountForCourse;
    @Value("${spring.application.course-limit-params.max-user-amount-for-course}")
    private int maxUserAmountForCourse;
    @Value("${spring.application.course-limit-params.max-solution-file-amount-for-course}")
    private int maxSolutionFileAmountForCourse;
    @Value("${spring.application.course-limit-params.max-file-amount-for-course-material}")
    private int maxFileAmountForCourseMaterial;
    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    private final CourseUserRepository courseUserRepository;

    public boolean isTaskAndMaterialFileAmountExceedsLimit(UUID courseId, int fileAmountToUpload){
        return calculateTaskAndMaterialFileAmountForCourse(courseId) + fileAmountToUpload
                > maxTaskAndMaterialFileAmountForCourse;
    }

    public boolean isFileAmountForCourseMaterialExceedsLimit(UUID courseId, UUID courseMaterialId, int fileAmountToUpload){
        Path courseBase = Paths.get(basePath, "courses", courseId.toString());
        Path materialsDir = courseBase.resolve("course_materials");
        Path materialDir = materialsDir.resolve(courseMaterialId.toString());

        long fileCount = countFiles(materialDir);

        return fileCount + fileAmountToUpload > maxFileAmountForCourseMaterial;
    }

    public Long calculateTaskAndMaterialFileAmountForCourse(UUID courseId){
        Path courseBase = Paths.get(basePath, "courses", courseId.toString());
        Path materialsDir = courseBase.resolve("course_materials");
        Path tasksDir = courseBase.resolve("tasks");

        long courseMaterialFileCount = countFiles(materialsDir);
        long taskFileCount = countFiles(tasksDir);

        return courseMaterialFileCount + taskFileCount;
    }

    private long countFiles(Path dir) {
        if (!Files.exists(dir)) {
            return 0L;
        }

        try (Stream<Path> files = Files.walk(dir)) {
            return files
                    .filter(Files::isRegularFile)
                    .count();
        } catch (IOException ex) {
            throw new IOEx(
                    "Ошибка на сервере",
                    "Ошибка при подсчете файлов"
            );
        }
    }

    public boolean isUserAmountForCourseExceedsLimit(int newUsers, UUID courseId) throws EntityNotFoundException {
        int courseUserCount = courseUserRepository.getUserCountForCourseByCourseId(courseId);
        return newUsers + courseUserCount > maxUserAmountForCourse;
    }
}