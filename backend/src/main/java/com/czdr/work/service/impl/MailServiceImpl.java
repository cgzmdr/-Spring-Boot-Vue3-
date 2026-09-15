package com.czdr.work.service.impl;

import com.czdr.work.service.MailService;
import com.czdr.work.service.RedisService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private final RedisService redisService;
    private static final String SMS_CAPTCHA_PREFIX = "sys:sms:captcha:";
    private static final long EXPIRE_TIME = 10;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendMail(String to, String subject, String verifyCode) {
        try {
            Path templatePath = Paths.get(System.getProperty("user.dir"), "dist", "sendemail.html");
            String htmlContent = Files.readString(templatePath, StandardCharsets.UTF_8);
            String finalContent = htmlContent.replace("{{code}}", verifyCode);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(finalContent, true);
            mailSender.send(message);
            redisService.setString(SMS_CAPTCHA_PREFIX+to,verifyCode,EXPIRE_TIME, TimeUnit.MINUTES);
        } catch (IOException e) {
            throw new RuntimeException("读取邮件模板失败，请检查 dist/sendemail.html 是否存在", e);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html == null ? "" : html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public boolean verifyCode(String account, String code) {
        if (code == null || account == null) {
            return false;
        }
        String redisKey = SMS_CAPTCHA_PREFIX + account;
        Object cachedCode = redisService.getString(redisKey);
        if (cachedCode != null && cachedCode.toString().equals(code)) {
            redisService.delete(redisKey);
            return true;
        }
        return false;
    }
}
