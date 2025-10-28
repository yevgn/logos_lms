package ru.antonov.oauth2_social.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskUserKey implements Serializable {
    @Column(name = "task_id", columnDefinition = "UUID")
    private UUID taskId;

    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o == null || this.getClass() != o.getClass() ) return false;
        TaskUserKey that = (TaskUserKey) o;
        return Objects.equals(getTaskId(), that.getTaskId()) && Objects.equals(getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaskId(), getUserId());
    }

    @Override
    public String toString() {
        return "TaskUserKey{" +
                "taskId=" + taskId +
                ", userId=" + userId +
                '}';
    }
}
