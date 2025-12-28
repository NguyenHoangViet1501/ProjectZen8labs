package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {

        String verifyUrl = "http://localhost:8080/api/auth/verify?token=" + token;

        String subject = "Xác nhận đăng ký tài khoản";

        String content = """
                Xin chào,

                Cảm ơn bạn đã đăng ký tài khoản.
                Vui lòng click vào link dưới đây để xác nhận email:

                %s

                Link sẽ hết hạn sau 24 giờ.
                """.formatted(verifyUrl);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }
}
