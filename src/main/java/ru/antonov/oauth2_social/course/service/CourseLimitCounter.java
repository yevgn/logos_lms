package ru.antonov.oauth2_social.course.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.antonov.oauth2_social.course.repository.CourseUserRepository;
import ru.antonov.oauth2_social.exception.FileNotFoundEx;
import ru.antonov.oauth2_social.exception.IOEx;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.solution.repository.SolutionRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Optional;
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
    @Value("${spring.application.course-limit-params.max-file-amount-for-solution}")
    private int maxFileAmountForSolution;
    @Value("${spring.application.course-limit-params.max-comment-amount-for-solution}")
    private int maxCommentAmountForSolution;
    @Value("${spring.application.file-storage.base-path}")
    private String basePath;

    private final CourseUserRepository courseUserRepository;
    private final SolutionRepository solutionRepository;

    public boolean isTaskAndMaterialFileAmountForCourseExceedsLimit(UUID courseId, int fileAmountToUpload){
        return calculateTaskAndMaterialFileAmountForCourse(courseId) + fileAmountToUpload
                > maxTaskAndMaterialFileAmountForCourse;
    }

    public boolean isSolutionFileAmountForCourseExceedsLimit(UUID courseId, int fileAmountToUpload){
        return calculateSolutionFileAmountForCourse(courseId) + fileAmountToUpload > maxSolutionFileAmountForCourse;
    }

    public boolean isCommentAmountForSolutionExceedsLimit(UUID solutionId, int commentAmountToAdd){
        return calculateCommentAmountForSolution(solutionId) + commentAmountToAdd > maxCommentAmountForSolution;
    }

    private int calculateCommentAmountForSolution(UUID solutionId) {
        Optional<Solution> solutionOpt = solutionRepository.findById(solutionId);
        return solutionOpt.map(solution -> solution.getComments().size()).orElse(0);
    }

    public boolean isFileAmountForCourseMaterialExceedsLimit(UUID courseId, UUID courseMaterialId, int fileAmountToUpload){
        Path courseBase = Paths.get(basePath, "courses", courseId.toString());
        Path materialsDir = courseBase.resolve("course_materials");
        Path materialDir = materialsDir.resolve(courseMaterialId.toString());

        long fileCount = countFiles(materialDir);

        return fileCount + fileAmountToUpload > maxFileAmountForCourseMaterial;
    }

    public boolean isFileAmountForTaskExceedsLimit(UUID courseId, UUID taskId, int fileAmountToUpload){
        Path courseBase = Paths.get(basePath, "courses", courseId.toString());
        Path tasksDir = courseBase.resolve("tasks");
        Path taskDir = tasksDir.resolve(taskId.toString());

        long fileCount = countFiles(taskDir);

        return fileCount + fileAmountToUpload > maxFileAmountForCourseMaterial;
    }

    public boolean isFileAmountForSolutionExceedsLimit(UUID courseId, UUID taskId, UUID solutionId, int fileAmountToUpload){
        Path courseBase = Paths.get(basePath, "courses", courseId.toString());
        Path tasksDir = courseBase.resolve("tasks");
        Path taskDir = tasksDir.resolve(taskId.toString());
        Path solutionsDir = taskDir.resolve("solutions");
        Path solutionDir = solutionsDir.resolve(solutionId.toString());

        long fileCount = countFiles(solutionDir);

        return fileCount + fileAmountToUpload > maxFileAmountForSolution;
    }

    public Long calculateTaskAndMaterialFileAmountForCourse(UUID courseId){
        Path coursePath = Paths.get(basePath, "courses", String.valueOf(courseId));

        if (!Files.exists(coursePath) || !Files.isDirectory(coursePath)) {
            throw new FileNotFoundEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при подсчете task & material файлов для курса %s. Директории %s не существует",
                            courseId, coursePath)
            );
        }

        try (Stream<Path> paths = Files.walk(coursePath)) {
            return paths
                    .filter(Files::isRegularFile) // Только файлы
                    .filter(path -> !path.toString().contains(File.separator + "solutions" + File.separator))
                    .count();
        } catch (IOException ex){
            throw new IOEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при подсчете task & material файлов для курса %s", courseId)
            );
        }
    }

    public Long calculateSolutionFileAmountForCourse(UUID courseId){
        Path coursePath = Paths.get(basePath, "courses", String.valueOf(courseId), "tasks");

        if (!Files.exists(coursePath) || !Files.isDirectory(coursePath)) {
            throw new FileNotFoundEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при подсчете solution файлов для курса %s. Директории %s не существует",
                            courseId, coursePath)
            );
        }

        try (Stream<Path> paths = Files.walk(coursePath)) {
            return paths
                    .filter(Files::isRegularFile) // Только файлы
                    .filter(path -> path.toString().contains(File.separator + "solutions" + File.separator))
                    .count();
        } catch (IOException ex){
            throw new IOEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при подсчете solution файлов для курса %s", courseId)
            );
        }
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