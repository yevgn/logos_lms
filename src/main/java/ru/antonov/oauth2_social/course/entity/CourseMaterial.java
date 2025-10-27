package ru.antonov.oauth2_social.course.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_materials",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"course_id", "topic"})
        })
public class CourseMaterial {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    // если удалить юзера, материалы должны остаться
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Setter(AccessLevel.NONE)
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_changed_at")
    private LocalDateTime lastChangedAt;

    @Column(name = "course_material_content", columnDefinition = "varchar")
    @Convert(converter = CourseMaterialContentJsonConverter.class)
    private List<Content> content;

    // ТИПА ОБЩЕЕ НАЗВАНИЕ (ЛЕКЦИИ, СЕМИНАР И Т.Д)
    private String topic;

    public void addCourseMaterialContent(List<Content> newContent){
        if(this.content != null){
            this.content.addAll(newContent);
        } else{
            content = new ArrayList<>(newContent);
        }
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
        CourseMaterial courseMaterial = (CourseMaterial) o;
        return getId() != null && Objects.equals(getId(), courseMaterial.getId());
    }

    @Override
    public String toString() {
        return "CourseMaterial{" +
                "id=" + id +
                ", course_id=" + course.getId() +
                ", user_id=" + user.getId() +
                ", publishedAt=" + publishedAt +
                ", lastChangedAt=" + lastChangedAt +
                ", courseMaterialContent=" + content +
                ", topic='" + topic + '\'' +
                '}';
    }
}
