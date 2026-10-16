package com.equestrian.equis.service.mail;

import com.equestrian.equis.common.config.MailSettings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

// Sends through the SMTP server in spring.mail.* (Gmail in the trial). MailException reaches the caller.
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
public class SmtpNotificationSender implements NotificationSender {
    private final JavaMailSender mailSender;
    private final MailSettings settings;

    public SmtpNotificationSender(JavaMailSender mailSender, MailSettings settings) {
        this.mailSender = mailSender;
        this.settings = settings;
    }

    @Override
    public void send(OutgoingEmail email) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(settings.from(), settings.fromName());
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.text(), email.html());
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("Could not build the email", e);
        }
        mailSender.send(message);
    }
}
