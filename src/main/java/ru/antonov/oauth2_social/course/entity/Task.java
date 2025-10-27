package ru.antonov.oauth2_social.course.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.antonov.oauth2_social.course.exception.IllegalTaskDeadlineEx;
import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tasks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"title", "course_id"})
        }
)
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User creator;

    private String title;

    @Setter(AccessLevel.NONE)
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_changed_at")
    private LocalDateTime lastChangedAt;

    @Column(name = "to_submit_at")
    private LocalDateTime toSubmitAt;

    private String description;

    @Column(name = "is_for_everyone")
    private boolean isForEveryone = true;

    @Column(name = "task_content", columnDefinition = "varchar")
    @Convert(converter = TaskContentJsonConverter.class)
    private Map<String, Object> taskContent;

    @OneToMany(
            mappedBy = "task",
            cascade = {CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private Set<Solution> solutions;

    @Column(name = "is_assessed")
    private boolean isAssessed = true;

    @PrePersist
    private void init(){
        publishedAt = LocalDateTime.now();
        if(toSubmitAt != null && toSubmitAt.isBefore(LocalDateTime.now())){
            throw new IllegalTaskDeadlineEx(
                    String.format("Ошибка при сохранении Course. Выбрано неправильное время для срока сдачи: %s", toSubmitAt),
                    String.format("Выбрано неправильное время для срока сдачи: %s", toSubmitAt)
            );
        }
    }

    @PreUpdate
    private void update(){
        if(toSubmitAt != null && toSubmitAt.isBefore(LocalDateTime.now())){
            throw new IllegalTaskDeadlineEx(
                    String.format("Ошибка при сохранении Course. Выбрано неправильное время для срока сдачи: %s", toSubmitAt),
                    String.format("Выбрано неправильное время для срока сдачи: %s", toSubmitAt)
            );
        }
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
        Task task = (Task) o;
        return getId() != null && Objects.equals(getId(), task.getId());
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
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", course_id=" + course.getId() +
                ", creator_id=" + creator.getId() +
                ", title='" + title + '\'' +
                ", publishedAt=" + publishedAt +
                ", lastChangedAt=" + lastChangedAt +
                ", toSubmitAt=" + toSubmitAt +
                ", description='" + description + '\'' +
                ", isForEveryone=" + isForEveryone +
                ", taskContent=" + taskContent +
                ", isAssessed=" + isAssessed +
                '}';
    }
}
