package ru.antonov.oauth2_social.auth.service;

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
    @Value("${spring.application.url.reset-password}")
    private String resetPasswordUrl;
    @Value("${spring.application.url.reset-2fa}")
    private String resetTfaUrl;
    @Value("${spring.application.sender-email}")
    private String senderEmail;

    private final JavaMailSender mailSender;

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

    public void sendMailForPasswordReset(User user, String resetPasswordToken) {
        String subject = "Сброс пароля в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                "<p>Перейдите по ссылке, чтобы сбросить пароль:</p>" +
                "<p><a href=\"%s?token=%s&user_email=%s\">Подтвердить</a></p>",
                user.getSurname(), user.getName(), user.getPatronymic(), resetPasswordUrl, resetPasswordToken, user.getEmail());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendMailForTfaReset(User user, String resetTfaToken) {
        String subject = "Сброс 2FA секретного ключа в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Перейдите по ссылке, чтобы сбросить 2FA секрет:</p>" +
                        "<p><a href=\"%s?token=%s&user_email=%s\">Подтвердить</a></p>",
                user.getSurname(), user.getName(), user.getPatronymic(), resetTfaUrl, resetTfaToken, user.getEmail());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendTfaSuccessfulResetNotification(User user) {
        String subject = "Ваш 2FA секретный ключ в Logos LMS был изменен";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Ваш 2FA секретный ключ был успешно изменен</p>",
                user.getSurname(), user.getName(), user.getPatronymic());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendPasswordSuccessfulResetNotification(User user) {
        String subject = "Ваш пароль в Logos LMS был изменен";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Пароль от вашей учетной записи был успешно изменен</p>",
                user.getSurname(), user.getName(), user.getPatronymic());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendTfaSuccessfulDisabledNotification(User user) {
        String subject = "Отключение двухфакторной аутентификации в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Двухфакторная аутентификация вашей учетной записи была успешно отключена</p>",
                user.getSurname(), user.getName(), user.getPatronymic());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendTfaSuccessfulEnabledNotification(User user) {
        String subject = "Активация двухфакторной аутентификации в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Двухфакторная аутентификация вашей учетной записи была успешно активирована</p>",
                user.getSurname(), user.getName(), user.getPatronymic());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }
}
