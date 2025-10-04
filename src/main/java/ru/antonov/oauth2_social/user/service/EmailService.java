package ru.antonov.oauth2_social.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.antonov.oauth2_social.exception.MailAuthenticationEx;
import ru.antonov.oauth2_social.exception.MailSendEx;
import ru.antonov.oauth2_social.exception.MessagingEx;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    @Value("${spring.application.url.activate-account}")
    private String activateAccountUrl;
    @Value("${spring.application.sender-email}")
    private String senderEmail;

    private final JavaMailSender mailSender;

    public void sendMailForAccountActivation(User user, String accountActivationToken){
        String subject = "Активация аккаунта в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Перейдите по ссылке, чтобы активировать ваш аккаунт. После активации вы сможете пользоваться " +
                        "всеми услугами сервиса</p>" +
                        "<p><a href=\"%s?token=%s&user_email=%s\">Подтвердить</a></p>",
                user.getSurname(), user.getName(), user.getPatronymic(), activateAccountUrl, accountActivationToken, user.getEmail());

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
