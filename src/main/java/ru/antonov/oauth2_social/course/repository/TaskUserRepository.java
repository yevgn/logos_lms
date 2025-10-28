package ru.antonov.oauth2_social.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.course.entity.TaskUser;
import ru.antonov.oauth2_social.course.entity.TaskUserKey;

@Repository
public interface TaskUserRepository extends JpaRepository<TaskUser, TaskUserKey> {
}
