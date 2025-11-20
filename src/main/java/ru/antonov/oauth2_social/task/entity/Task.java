package ru.antonov.oauth2_social.task.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.antonov.oauth2_social.common.Content;
import ru.antonov.oauth2_social.course.entity.*;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.task.exception.IllegalTaskDeadlineEx;
import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;
import java.util.*;

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
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User creator;

    private String title;

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
    private List<Content> content = new ArrayList<>();

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "task",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private Set<TaskUser> taskUsers = new HashSet<>();

    @OneToMany(
            mappedBy = "task",
            cascade = {CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private Set<Solution> solutions = new HashSet<>();

    @Column(name = "is_assessed")
    private boolean isAssessed = true;

    public void addContent(List<Content> content){
        this.content.addAll(content);
    }

    public void addContent(Content content){
        this.content.add(content);
    }

    @PrePersist
    private void checkToSubmitAt(){
        if(toSubmitAt != null && !toSubmitAt.isAfter(LocalDateTime.now().plusHours(1))){
            throw new IllegalTaskDeadlineEx(
                    "Неправильно время дедлайна. Дедлайн должен быть хотя бы на час позже настоящего времени",
                    String.format("Ошибка обновления Task. Выбрано неправильное время для срока сдачи: %s", toSubmitAt)
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
                ", content=" + content +
                ", isAssessed=" + isAssessed +
                '}';
    }
}
