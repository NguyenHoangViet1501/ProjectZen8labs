package com.backend.quanlytasks.service;

import java.util.Map;

/**
 * Service interface cho Firebase Cloud Messaging operations
 */
public interface FirebaseService {

    /**
     * Gửi push notification đến device
     * 
     * @param token FCM token của device
     * @param title Tiêu đề thông báo
     * @param body  Nội dung thông báo
     * @param data  Dữ liệu bổ sung (optional)
     * @return true nếu gửi thành công, false nếu thất bại
     */
    boolean sendPushNotification(String token, String title, String body, Map<String, String> data);
}
