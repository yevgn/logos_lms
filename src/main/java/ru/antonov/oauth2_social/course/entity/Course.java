package ru.antonov.oauth2_social.course.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.antonov.oauth2_social.user.entity.Institution;
import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"code"}),
                @UniqueConstraint(columnNames = {"name", "institution_id"})
})
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    private String name;

    private String code;

    @Setter(AccessLevel.NONE)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @OneToMany(mappedBy = "course", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<CourseUser> courseUsers;

    @OneToMany(mappedBy = "course", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Task> tasks;

    @OneToMany(mappedBy = "course",  cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Solution> solutions;

    public void addCourseUsers(Set<CourseUser> courseUsers){
        this.courseUsers.addAll(courseUsers);
        courseUsers.forEach(cu -> cu.setCourse(this));
    }

    public void setCourseUsers(Set<CourseUser> courseUsers){
        this.courseUsers = courseUsers;
        courseUsers.forEach(cu -> cu.setCourse(this));
    }

    public void removeUsers(List<User> users){
        users.forEach(u -> courseUsers.remove(
                CourseUser
                        .builder()
                        .id(CourseUserKey.builder().userId(u.getId()).courseId(getId()).build())
                        .build()
        ));
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode()
                : getClass().hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer()
                .getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Course course = (Course) o;
        return getId() != null && Objects.equals(getId(), course.getId());
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", createdAt=" + createdAt +
                ", institution_id=" + institution.getId() +
                '}';
    }
}
