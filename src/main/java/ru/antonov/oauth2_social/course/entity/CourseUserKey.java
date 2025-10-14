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
public class CourseUserKey implements Serializable {
    @Column(name = "course_id", columnDefinition = "UUID")
    private UUID courseId;

    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o == null || this.getClass() != o.getClass() ) return false;
        CourseUserKey that = (CourseUserKey) o;
        return Objects.equals(getCourseId(), that.getCourseId()) && Objects.equals(getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCourseId(), getUserId());
    }

    @Override
    public String toString() {
        return "CourseUserKey{" +
                "courseId=" + courseId +
                ", userId=" + userId +
                '}';
    }
}
