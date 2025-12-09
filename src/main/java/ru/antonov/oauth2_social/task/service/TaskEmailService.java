package ru.antonov.oauth2_social.task.service;

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
import ru.antonov.oauth2_social.common.exception.MailAuthenticationEx;
import ru.antonov.oauth2_social.common.exception.MailSendEx;
import ru.antonov.oauth2_social.common.exception.MessagingEx;
import ru.antonov.oauth2_social.task.entity.Task;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskEmailService {
    @Value("${spring.application.sender-email}")
    private String senderEmail;

    @Value("${spring.application.url.get-task-info}")
    private String getTaskInfoUrl;

    private final JavaMailSender mailSender;

    @Async
    public void sendTaskUploadedNotification(User user, Task task){
        String subject = "Появилось новое задание!";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>В курсе %s появилось новое задание!" +
                        "<p><a href=\"%s\">Перейти к заданию</a></p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic(),
                task.getCourse().getName(), getTaskInfoUrl);

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
