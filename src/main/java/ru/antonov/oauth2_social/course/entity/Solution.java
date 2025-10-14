package ru.antonov.oauth2_social.course.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "solutions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "task_id"})
})
public class Solution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @Setter(AccessLevel.NONE)
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "last_changed_at")
    private LocalDateTime lastChangedAt;

    @Enumerated(EnumType.STRING)
    private SolutionStatus status;

    private Integer mark;

    @Column(name = "solution_content", columnDefinition = "varchar")
    @Convert(converter = SolutionContentJsonConverter.class)
    private Map<String, Object> solutionContent;

    @PrePersist
    private void init(){
        submittedAt = LocalDateTime.now();
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
        Solution solution = (Solution) o;
        return getId() != null && Objects.equals(getId(), solution.getId());
    }

    @Override
    public String toString() {
        return "Solution{" +
                "id=" + id +
                ", user_id=" + user.getId() +
                ", task_id=" + task.getId() +
                ", submittedAt=" + submittedAt +
                ", lastChangedAt=" + lastChangedAt +
                ", status=" + status +
                ", mark=" + mark +
                ", solutionContent=" + solutionContent +
                '}';
    }
}
