/**
 * Firebase Cloud Messaging Module
 * Xử lý đăng ký và nhận push notification trên web
 */

const FIREBASE_CONFIG = {
    apiKey: "AIzaSyBRui7s74I-wXLqeDjFxG8-wdFgr_1LmeA",
    authDomain: "noitificationzen8labs.firebaseapp.com",
    projectId: "noitificationzen8labs",
    storageBucket: "noitificationzen8labs.firebasestorage.app",
    messagingSenderId: "243671672460",
    appId: "1:243671672460:web:1083c874ba0f3eb31c3da9"
};

const VAPID_KEY = 'BGugbb2bx3xHlh1syTChBVG-hcQGNqO5dTtgX8uB_63X9QBKlu9vaZ4d9u0PJnVDLa6L3_hNnY6eARNhkoT_woI';

let messaging = null;

/**
 * Khởi tạo Firebase và đăng ký nhận push notification
 */
async function initFirebaseMessaging() {
    try {
        // Kiểm tra browser hỗ trợ
        if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
            console.warn('Browser không hỗ trợ push notification');
            return;
        }

        // Load Firebase SDK dynamically
        await loadFirebaseScripts();

        // Initialize Firebase
        if (!firebase.apps.length) {
            firebase.initializeApp(FIREBASE_CONFIG);
        }

        messaging = firebase.messaging();

        // Register Service Worker
        const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
        console.log('Service Worker registered:', registration);

        // Request permission
        const permission = await Notification.requestPermission();
        if (permission !== 'granted') {
            console.warn('Notification permission denied');
            return;
        }

        // Get FCM Token
        const token = await messaging.getToken({
            vapidKey: VAPID_KEY,
            serviceWorkerRegistration: registration
        });

        if (token) {
            console.log('FCM Token:', token);
            await sendTokenToServer(token);
        }

        // Handle foreground messages
        messaging.onMessage((payload) => {
            console.log('Foreground message received:', payload);
            showNotification(payload);
        });

        console.log('Firebase Messaging initialized successfully!');

    } catch (error) {
        console.error('Error initializing Firebase Messaging:', error);
    }
}

/**
 * Load Firebase scripts dynamically
 */
function loadFirebaseScripts() {
    return new Promise((resolve, reject) => {
        if (window.firebase) {
            resolve();
            return;
        }

        const script1 = document.createElement('script');
        script1.src = 'https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js';
        script1.onload = () => {
            const script2 = document.createElement('script');
            script2.src = 'https://www.gstatic.com/firebasejs/10.7.0/firebase-messaging-compat.js';
            script2.onload = resolve;
            script2.onerror = reject;
            document.head.appendChild(script2);
        };
        script1.onerror = reject;
        document.head.appendChild(script1);
    });
}

/**
 * Gửi FCM token về server
 */
async function sendTokenToServer(token) {
    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

        const headers = {
            'Content-Type': 'application/json'
        };

        if (csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/fcm-token', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ fcmToken: token })
        });

        if (response.ok) {
            console.log('FCM token saved to server');
        } else {
            console.error('Failed to save FCM token:', response.status);
        }
    } catch (error) {
        console.error('Error sending token to server:', error);
    }
}

/**
 * Hiển thị notification khi app đang mở (foreground)
 */

/**
 * Hiển thị notification bằng Bootstrap Toast
 */
function showToast(title, body) {
    const toastLiveExample = document.getElementById('liveToast');
    const toastTitle = document.getElementById('toastTitle');
    const toastBody = document.getElementById('toastBody');

    if (toastLiveExample && toastTitle && toastBody) {
        toastTitle.textContent = title;
        toastBody.textContent = body;

        // Use Bootstrap global object (window.bootstrap)
        const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastLiveExample);
        toastBootstrap.show();

        // Play notification sound (optional)
        // const audio = new Audio('/audio/notification.mp3');
        // audio.play().catch(e => console.log('Audio play failed', e));

        // Tự động reload trang sau 2 giây để cập nhật dữ liệu
        // Chỉ reload nếu user không đang nhập liệu (focus vào input/textarea)
        setTimeout(() => {
            const activeElement = document.activeElement;
            const isInputActive = activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA');

            if (!isInputActive) {
                window.location.reload();
            } else {
                console.log('User đang nhập liệu, không tự động reload');
                // Có thể thêm tính năng hiện nút "Reload" nếu cần
            }
        }, 2000);

    } else {
        console.warn('Toast elements not found, falling back to native notification');
        if (Notification.permission === 'granted') {
            new Notification(title, { body: body });
        }
    }
}

/**
 * Hiển thị notification khi app đang mở (foreground)
 */
function showNotification(payload) {
    const title = payload.notification?.title || 'Thông báo mới';
    const body = payload.notification?.body || '';

    // Hiển thị toast
    showToast(title, body);

    // Refresh notification badge
    updateNotificationBadge();
}

/**
 * Cập nhật badge số thông báo chưa đọc
 */
function updateNotificationBadge() {
    const badge = document.getElementById('notification-badge');
    if (badge) {
        const currentCount = parseInt(badge.textContent) || 0;
        badge.textContent = currentCount + 1;
        badge.style.display = 'inline';
    }
}


// Auto-initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initFirebaseMessaging);
} else {
    initFirebaseMessaging();
}
