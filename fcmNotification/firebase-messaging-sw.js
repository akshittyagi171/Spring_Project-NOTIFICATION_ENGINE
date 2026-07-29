importScripts('https://www.gstatic.com/firebasejs/10.8.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.8.1/firebase-messaging-compat.js');

firebase.initializeApp({
 apiKey: "xxxxxxxxxxxxxxx",
 authDomain: "xxxxxxxxxxxxxxx",
 projectId: "xxxxxxxxxxxxxxx",
 storageBucket: "xxxxxxxxxxxxxxx",
 messagingSenderId: "xxxxxxxxxxxxxxx",
 appId: "xxxxxxxxxxxxxxx",
 measurementId: "xxxxxxxxxxxxxxx"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage(function(payload) {
  console.log('Background message received.', payload);
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: payload.notification.image || '/default-icon.png'
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});