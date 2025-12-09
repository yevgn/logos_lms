package ru.antonov.oauth2_social.solution.service;

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
import ru.antonov.oauth2_social.solution.entity.Solution;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionEmailService {
    @Value("${spring.application.sender-email}")
    private String senderEmail;

    @Value("${spring.application.url.get-solution-info}")
    private String getSolutionInfoUrl;

    private final JavaMailSender mailSender;

    @Async
    public void sendSolutionReviewedNotification(User user, Solution solution){
        String subject = "Появилась оценка решения!";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Пользователь %s %s %s проверил ваше решение к заданию \"%s\" в рамках курса \"%s\"!" +
                        "<p><a href=\"%s\">Перейти к решению</a></p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic(),
                solution.getReviewer().getSurname(), solution.getReviewer().getName(),
                solution.getReviewer().getPatronymic() == null ? "" : solution.getReviewer().getPatronymic(),
                solution.getTask().getTitle(), solution.getTask().getCourse().getName(), getSolutionInfoUrl);

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    @Async
    public void sendSolutionUploadedNotification(User user, Solution solution){
        String subject = "Ученик загрузил решение к заданию";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>В курсе \"%s\" пользователь %s %s прикрепил решение к заданию \"%s\"" +
                        "<p><a href=\"%s\">Перейти к проверке</a></p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic(),
                solution.getTask().getCourse().getName(), solution.getUser().getSurname(), solution.getUser().getName(),
                solution.getTask().getTitle(), getSolutionInfoUrl);

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

