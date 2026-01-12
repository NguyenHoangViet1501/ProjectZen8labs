// Firebase Messaging Service Worker
// Nhận push notification khi browser đóng hoặc chạy nền

importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-messaging-compat.js');

// Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyBRui7s74I-wXLqeDjFxG8-wdFgr_1LmeA",
    authDomain: "noitificationzen8labs.firebaseapp.com",
    projectId: "noitificationzen8labs",
    storageBucket: "noitificationzen8labs.firebasestorage.app",
    messagingSenderId: "243671672460",
    appId: "1:243671672460:web:1083c874ba0f3eb31c3da9"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Initialize Messaging
const messaging = firebase.messaging();

// Handle background messages
messaging.onBackgroundMessage((payload) => {
    console.log('[firebase-messaging-sw.js] Received background message:', payload);

    const notificationTitle = payload.notification?.title || 'Thông báo mới';
    const notificationOptions = {
        body: payload.notification?.body || '',
        tag: payload.data?.notificationId || 'default',
        data: payload.data,
        requireInteraction: true,
        actions: [
            { action: 'view', title: 'Xem chi tiết' },
            { action: 'dismiss', title: 'Bỏ qua' }
        ]
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification click
self.addEventListener('notificationclick', (event) => {
    console.log('[firebase-messaging-sw.js] Notification click:', event);

    event.notification.close();

    if (event.action === 'view' || !event.action) {
        // Open the app or focus if already open
        const urlToOpen = event.notification.data?.url || '/tasks';

        event.waitUntil(
            clients.matchAll({ type: 'window', includeUncontrolled: true })
                .then((clientList) => {
                    // Check if there's already a window open
                    for (const client of clientList) {
                        if (client.url.includes('/tasks') && 'focus' in client) {
                            return client.focus();
                        }
                    }
                    // Open new window if none exists
                    if (clients.openWindow) {
                        return clients.openWindow(urlToOpen);
                    }
                })
        );
    }
});
