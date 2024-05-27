package com.project.alfaf;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class FirebaseNotificationService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseNotificationService";
    private static final String CHANNEL_ID = "FirebaseNotificationChannel";
    private static final String SERVER_URL = "http://100.75.230.21:5000";
    private static final String FILE_NAME = "user_info.txt";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Firebase Notification Service")
                .setContentText("Listening for Firebase messages")
                .setSmallIcon(R.drawable.alert_icon)
                .build();
        startForeground(1, notification);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }

            // Get new FCM registration token
            String token = task.getResult();
            sendRegistrationToServer(token);
        });
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void sendRegistrationToServer(String token) {
        new Thread(() -> {
            try {
                String phoneNumber = getPhoneNumberFromFile();

                if (phoneNumber != null) {
                    URL url = new URL(SERVER_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

                    String postData = "register_token " + phoneNumber + " " + token;

                    OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                    out.write(postData);
                    out.flush();
                    out.close();

                    int responseCode = conn.getResponseCode();
                    Log.d(TAG, "Response Code: " + responseCode);

                    conn.disconnect();
                } else {
                    Log.e(TAG, "Phone number is null, could not send registration to server.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getPhoneNumberFromFile() {
        File file = new File(getFilesDir(), FILE_NAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader reader = new BufferedReader(isr)) {
                String line = reader.readLine();
                if (line != null) {
                    String[] userInfo = line.split(" ");
                    if (userInfo.length == 2) {
                        return userInfo[1];
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "User info file does not exist.");
        }
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Firebase Notification Service";
            String description = "Channel for Firebase Notification Service";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
