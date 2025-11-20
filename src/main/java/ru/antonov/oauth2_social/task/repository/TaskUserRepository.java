package ru.antonov.oauth2_social.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.task.entity.TaskUser;
import ru.antonov.oauth2_social.task.entity.TaskUserKey;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskUserRepository extends JpaRepository<TaskUser, TaskUserKey> {
    @Query(value = """
            SELECT tu.user u FROM TaskUser tu
            JOIN FETCH tu.user.institution
            JOIN FETCH tu.user.group
            WHERE tu.task.id = :taskId
            """)
    List<User> findAllUsersByTaskId(UUID taskId);
}
