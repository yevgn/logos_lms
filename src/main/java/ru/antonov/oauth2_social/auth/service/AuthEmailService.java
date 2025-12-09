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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.antonov.oauth2_social.config.MailSendFailure;
import ru.antonov.oauth2_social.config.MailSendFailureRepository;
import ru.antonov.oauth2_social.common.exception.MailAuthenticationEx;
import ru.antonov.oauth2_social.common.exception.MailSendEx;
import ru.antonov.oauth2_social.common.exception.MessagingEx;
import ru.antonov.oauth2_social.user.entity.User;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEmailService {
    @Value("${spring.application.url.reset-password}")
    private String resetPasswordUrl;
    @Value("${spring.application.url.reset-2fa}")
    private String resetTfaUrl;
    @Value("${spring.application.url.activate-account}")
    private String activateAccountUrl;
    @Value("${spring.application.sender-email}")
    private String senderEmail;

    private final JavaMailSender mailSender;

    private final MailSendFailureRepository mailSendFailureRepository;

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

    @Async
    @Retryable(
            retryFor = {MailSendException.class, MailAuthenticationException.class, MessagingException.class},
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public void sendMailForAccountActivation(User user, String accountActivationToken){
        String subject = "Активация аккаунта в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Перейдите по ссылке, чтобы активировать ваш аккаунт. После активации вы сможете пользоваться " +
                        "всеми услугами сервиса</p>" +
                        "<p><a href=\"%s?token=%s&user_email=%s\">Подтвердить</a></p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic(),
                activateAccountUrl, accountActivationToken, user.getEmail());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendMailForPasswordReset(User user, String resetPasswordToken) {
        String subject = "Сброс пароля в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                "<p>Перейдите по ссылке, чтобы сбросить пароль:</p>" +
                "<p><a href=\"%s?token=%s&user_email=%s\">Подтвердить</a></p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic(),
                resetPasswordUrl, resetPasswordToken, user.getEmail());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendMailForTfaReset(User user, String resetTfaToken) {
        String subject = "Сброс 2FA секретного ключа в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Перейдите по ссылке, чтобы сбросить :</p>" +
                        "<p><a href=\"%s?token=%s&user_email=%s\">Подтвердить</a></p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic(),
                resetTfaUrl, resetTfaToken, user.getEmail());

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendTfaSuccessfulResetNotification(User user) {
            String subject = "Двухфакторная аутентификация в Logos LMS была отключена";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>На вашем аккаунте в Logos LMS была отключена двухфакторная аутентификация </p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic()
        );

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendPasswordSuccessfulResetNotification(User user) {
        String subject = "Ваш пароль в Logos LMS был изменен";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Пароль от вашей учетной записи был успешно изменен</p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic()
        );

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendTfaSuccessfulDisabledNotification(User user) {
        String subject = "Отключение двухфакторной аутентификации в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Двухфакторная аутентификация вашей учетной записи была успешно отключена</p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic()
        );

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    public void sendTfaSuccessfulEnabledNotification(User user) {
        String subject = "Активация двухфакторной аутентификации в Logos LMS";

        String htmlText = String.format("<p>Добрый день, %s %s %s!</p>" +
                        "<p>Двухфакторная аутентификация вашей учетной записи была успешно активирована</p>",
                user.getSurname(), user.getName(),
                user.getPatronymic() == null ? "" : user.getPatronymic()
        );

        sendEmail(subject, htmlText, new String[]{user.getEmail()});
    }

    @Recover
    public void recoverFromMailError(Exception ex, User user, String accountActivationToken) {
        log.error("Ошибка рассылки писем с ссылкой для подтверждения аккаунта. " +
                "Не удалось отправить письмо пользователю {} после всех попыток: {}", user.getEmail(), ex.getMessage());

        mailSendFailureRepository.save(
                MailSendFailure
                        .builder()
                        .user(user)
                        .failureDescription("Не удалось отправить письмо с ссылкой для подтверждения аккаунта")
                        .build()
        );
    }
}
