package ru.antonov.oauth2_social.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.antonov.oauth2_social.course.dto.TaskCreateRequestDto;
import ru.antonov.oauth2_social.course.dto.TaskCreateResponseDto;
import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.entity.Task;
import ru.antonov.oauth2_social.course.repository.TaskRepository;
import ru.antonov.oauth2_social.exception.EntityNotFoundEx;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;
    private final CourseService courseService;

    public TaskCreateResponseDto saveTask(User principal, UUID courseId, TaskCreateRequestDto request) {
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundEx(
                        String.format("Ошибка. Курса с id %s не существует", courseId),
                        String.format("Ошибка при добавлении задания в курс пользователем %s. " +
                                "Курса с id %s не существует", principal.getId(), courseId)
                ));
        // todo
        return null;
        // есть ли у таргет юзеров доступ к курсу
        // подготовка content
        // если для всех, сохраняем task, если нет - taskAssignments
        // запись файлов
    }


    public Task save(Task task){
        return taskRepository.saveAndFlush(task);
    }

    public Optional<Task> findById(UUID taskId){
        return taskRepository.findById(taskId);
    }

}
