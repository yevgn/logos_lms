package ru.antonov.oauth2_social.solution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "solution_comments")
public class SolutionComment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solution_id")
    private Solution solution;

    private String text;

    private LocalDateTime publishedAt;

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
        SolutionComment comment = (SolutionComment) o;
        return getId() != null && Objects.equals(getId(), comment.getId());
    }

    @Override
    public String toString() {
        return "SolutionComment{" +
                "id=" + id +
                ", author_id=" + author.getId() +
                ", solution_id=" + solution.getId() +
                ", text='" + text + '\'' +
                ", publishedAt=" + publishedAt +
                '}';
    }
}

