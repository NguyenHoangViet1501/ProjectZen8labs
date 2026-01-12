package com.backend.quanlytasks.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Cấu hình Firebase Admin SDK
 * Khởi tạo FirebaseApp khi ứng dụng start
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK đã được khởi tạo thành công");
            }
        } catch (IOException e) {
            log.error("Không thể khởi tạo Firebase Admin SDK: {}", e.getMessage());
            log.warn("Push notification sẽ không hoạt động. Vui lòng kiểm tra file firebase-service-account.json");
        }
    }
}
