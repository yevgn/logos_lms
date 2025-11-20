package ru.antonov.oauth2_social.course.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.antonov.oauth2_social.course.entity.Course;
import ru.antonov.oauth2_social.course.entity.CourseMaterial;
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.exception.MailAuthenticationEx;
import ru.antonov.oauth2_social.exception.MailSendEx;
import ru.antonov.oauth2_social.exception.MessagingEx;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseEmailService {
    @Value("${spring.application.sender-email}")
    private String senderEmail;

    @Value("${spring.application.url.get-course-info}")
    private String getCourseInfoUrl;
    @Value("${spring.application.url.get-course-material-info}")
    private String getCourseMaterialInfoUrl;


    private final JavaMailSender mailSender;

    @Async
    public void sendCourseJoinNotification(User user, Course course){
        String subject = "Вас записали на курс";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Вы были добавлены на курс %s в Logos LMS." +
                        "<p><a href=\"%s\">Перейти к курсу</a></p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic(),
                course.getName(), getCourseInfoUrl
        );

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendCourseMaterialUploadedNotification(User user, CourseMaterial material){
        String subject = "Появились новые учебные материалы!";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>В курсе %s появились новые учебные материалы \"%s\"!" +
                        "<p><a href=\"%s\">Перейти к материалам</a></p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic(),
                material.getCourse().getName(),
                material.getTopic(),
                getCourseMaterialInfoUrl);

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    private void sendEmail(String subject, String text, String[] sendTo) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(sendTo);
            helper.setSubject(subject);
            helper.setText(text, true);
            helper.setFrom(senderEmail);
            mailSender.send(message);
        } catch (MailSendException ex){
            throw new MailSendEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при отправке письма пользователям %s.\n%s", Arrays.toString(sendTo), ex.getMessage())
            );
        } catch (MailAuthenticationException ex){
            throw new MailAuthenticationEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при отправке письма пользователям %s\n%s", Arrays.toString(sendTo), ex.getMessage())
            );
        } catch (MessagingException ex){
            throw new MessagingEx(
                    "Ошибка на сервере",
                    String.format("Ошибка при отправке письма пользователям %s\n%s", Arrays.toString(sendTo), ex.getMessage())
            );
        }
    }
}
