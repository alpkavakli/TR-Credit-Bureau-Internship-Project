package com.findeks.miniscore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * SMTP üzerinden e-posta gönderir. Yalnızca doğrulama kodu için kullanılıyor.
 *
 * JavaMailSender bean'i, application.yml'de spring.mail.host tanımlı olduğu için HER ZAMAN
 * oluşur (kimlik bilgileri boş olsa bile). Böylece 2FA kapalıyken uygulama sorunsuz açılır;
 * gerçek gönderim sadece 2FA açıkken ve gönderildiğinde denenir.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.verification.from}")
    private String from;

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Findeks MiniScore doğrulama kodunuz");
        message.setText(
                "Giriş/kayıt doğrulama kodunuz: " + code + "\n\n"
                + "Bu kod kısa süre içinde geçerliliğini yitirecektir. "
                + "Bu işlemi siz yapmadıysanız e-postayı yok sayabilirsiniz.");
        try {
            mailSender.send(message);
            log.info("Doğrulama kodu gönderildi: {}", to);
        } catch (MailException ex) {
            // Kod/hedef gibi hassas bilgiyi değil, sadece nedeni logla.
            log.error("Doğrulama e-postası gönderilemedi ({}): {}", to, ex.getMessage());
            throw new IllegalStateException(
                    "Doğrulama e-postası gönderilemedi. Lütfen daha sonra tekrar deneyin.");
        }
    }
}
