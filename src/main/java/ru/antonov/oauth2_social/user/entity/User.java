package ru.antonov.oauth2_social.user.entity;

import jakarta.persistence.*;

import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.antonov.oauth2_social.config.MailSendFailure;
import ru.antonov.oauth2_social.course.entity.CourseMaterial;
import ru.antonov.oauth2_social.course.entity.CourseUser;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.task.entity.TaskUser;

import java.util.Collection;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"email"}))
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    private String email;

    private String surname;

    private String name;

    private String patronymic;

    private String password;

    private Integer age;

   // @ManyToOne( optional = false)
    @ManyToOne(optional = true)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @OneToMany(mappedBy = "user")
    private Set<CourseUser> courseUsers;

    @OneToMany(mappedBy = "user")
    private Set<Solution> solutions;

    @OneToMany(mappedBy = "user")
    private Set<CourseMaterial> courseMaterials;

    @OneToMany(mappedBy = "user")
    private Set<MailSendFailure> mailSendFailures;

    @OneToMany(mappedBy = "user")
    private Set<TaskUser> taskUsers;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_enabled")
    private boolean isEnabled = false;

    @Column(name = "tfa_secret")
    private String tfaSecret;

    @Column(name = "is_tfa_enabled")
    private boolean isTfaEnabled;

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
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
        User user = (User) o;
        return getId() != null && Objects.equals(getId(), ((User) o).getId());
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", surname='" + surname + '\'' +
                ", name='" + name + '\'' +
                ", patronymic='" + patronymic + '\'' +
                ", age=" + age +
                ", institution_id=" + institution.getId() +
                ", group=" + group +
                ", role=" + role +
                '}';
    }
}
