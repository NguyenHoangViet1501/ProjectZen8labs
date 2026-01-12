package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.service.FirebaseService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Implementation của FirebaseService
 * Gửi push notification thông qua Firebase Cloud Messaging
 */
@Service
@Slf4j
public class FirebaseServiceImpl implements FirebaseService {

    @Override
    public boolean sendPushNotification(String token, String title, String body, Map<String, String> data) {
        // Kiểm tra Firebase đã được khởi tạo chưa
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase chưa được khởi tạo. Không thể gửi push notification.");
            return false;
        }

        if (token == null || token.isEmpty()) {
            log.warn("FCM token không hợp lệ. Không thể gửi push notification.");
            return false;
        }

        try {
            // Tạo notification
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Tạo message
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification);

            // Thêm data nếu có
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();

            // Gửi message
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification đã gửi thành công. Message ID: {}", response);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error("Lỗi khi gửi push notification: {} - {}", e.getMessagingErrorCode(), e.getMessage());

            // Xử lý token không hợp lệ
            if (e.getMessagingErrorCode() != null) {
                switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED:
                    case INVALID_ARGUMENT:
                        log.warn("FCM token không hợp lệ hoặc đã hết hạn");
                        break;
                    default:
                        log.error("Lỗi FCM: {}", e.getMessagingErrorCode());
                }
            }
            return false;
        }
    }
}
