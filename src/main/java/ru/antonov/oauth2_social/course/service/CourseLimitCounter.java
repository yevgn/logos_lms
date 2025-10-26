package ru.antonov.oauth2_social.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseLimitCounter {
    @Value("${spring.application.course-limit-params.max-task-and-material-file-amount-for-course}")
    private int maxTaskAndMaterialFileAmountForCourse;
    @Value("${spring.application.course-limit-params.max-user-amount-for-course}")
    private int maxUserAmountForCourse;
    @Value("${spring.application.course-limit-params.max-solution-file-amount-for-course}")
    private int maxSolutionFileAmountForCourse;

   // private final CourseService courseService;

    private final String COURSE_DIR_TEMPLATE = "D:/courses/%s";

    public boolean isTaskAndMaterialFileAmountExceedsLimit(UUID courseId, int fileAmountToUpload){
        //return calculateFileAmountForCourse(courseId) + fileAmountToUpload > maxFileAmount;
        return false;
        // todo СДЕЛАТЬ
    }

    public boolean isUserAmountExceedsLimit(int newUsers, UUID courseId){
        //return courseUserService.calcUserAmountForCourse(courseId) + newUsers > maxUserAmountForCourse;
        return false;
        // todo СДЕЛАТЬ
    }

    public Long calculateFileAmountForCourse(UUID courseId){
//        String courseDir = String.format(COURSE_DIR_TEMPLATE, courseId);
//        try (Stream<Path> paths = Files.walk(Paths.get(courseDir))) {
//            return paths
//                    .filter( p ->
//                            Files.isRegularFile(p) && (p.getFileName().startsWith("st_") || p.getFileName().startsWith("t_")
//                                    || p.getFileName().startsWith("p_")) )
//                    .count();
//        } catch (IOException ex){
//
//            throw new InternalServerErrorEx("Ошибка на сервере");
//        }
        // todo СДЕЛАТЬ
        return 0L;
    }

    public Long calculateFreeStorageSpaceLeft(UUID courseId){
        // todo СДЕЛАТЬ
        return 0L;
    }
}