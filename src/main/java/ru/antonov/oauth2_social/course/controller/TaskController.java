package ru.antonov.oauth2_social.course.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.antonov.oauth2_social.course.service.TaskService;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TaskController{
    private final TaskService taskService;
}