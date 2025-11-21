package ru.antonov.oauth2_social.solution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.antonov.oauth2_social.common.Content;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.user.entity.User;

import java.time.LocalDateTime;
import java.util.*;

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
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private SolutionStatus status;

    private Integer mark;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "solution_content", columnDefinition = "varchar")
    @Convert(converter = SolutionContentJsonConverter.class)
    private List<Content> content = new ArrayList<>();

    @Column(name = "solution_comments", columnDefinition = "varchar")
    @Convert(converter = SolutionCommentsJsonConverter.class)
    private List<SolutionComment> comments = new ArrayList<>();

   @Version
    private Long version;

    public void addContent(List<Content> content){
        this.content.addAll(content);
    }

    public void addComments(List<SolutionComment> comments){
        this.comments.addAll(comments);
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
                ", status=" + status +
                ", mark=" + mark +
                ", content=" + content +
                '}';
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class SolutionComment{
        private UUID id;
        private UUID userId;
        private String text;
        private LocalDateTime publishedAt;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SolutionComment that = (SolutionComment) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }
}
