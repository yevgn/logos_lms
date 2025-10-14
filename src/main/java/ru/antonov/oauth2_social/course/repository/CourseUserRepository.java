package ru.antonov.oauth2_social.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import ru.antonov.oauth2_social.course.entity.CourseUser;
import ru.antonov.oauth2_social.course.entity.CourseUserKey;



@Repository
public interface CourseUserRepository extends JpaRepository<CourseUser, CourseUserKey> {
}
