package ru.antonov.oauth2_social.auth.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.antonov.oauth2_social.auth.exception.Qr2faGenerationEx;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthenticationService {
    @Value("${spring.application.name}")
    private String issuer;

    // Метод, генерирующий 2FA secret
    public String generateNewSecret() {
        return new DefaultSecretGenerator().generate();
    }

    // Метод, генерирующий QR-код для отображения секрета в браузере
    public String generateQrCodeImageUri(String secret, String userEmail) {
        // используется алгоритм хэширования HMAC-SHA1
        QrData data = new QrData.Builder()
                .label(issuer + ":" + userEmail)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData = new byte[0];
        try {
            imageData = generator.generate(data);
        } catch (QrGenerationException e) {
            throw new Qr2faGenerationEx(
                    "Ошибка на сервере",
                    String.format("Ошибка на сервере. Ошибка при генерации QR-кода с 2fa секретом для пользователя %s",
                            userEmail)
            );
        }

        return Utils.getDataUriForImage(imageData, generator.getImageMimeType());
    }

    // Проверка соответствия 2FA кода 2FA секрету
    public boolean isOtpValid(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

    public boolean isOtpNotValid(String secret, String code) {
        return !this.isOtpValid(secret, code);
    }
}
