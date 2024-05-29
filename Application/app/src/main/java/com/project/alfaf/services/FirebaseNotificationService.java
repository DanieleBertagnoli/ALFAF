package com.project.alfaf.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.project.alfaf.EmergencyMapActivity;
import com.project.alfaf.MainActivity;
import com.project.alfaf.SettingsActivity;
import com.project.alfaf.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class FirebaseNotificationService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseNotificationService";
    private static final String CHANNEL_ID = "FirebaseNotificationChannel";
    private static final String SERVER_URL = "http://100.75.230.21:5000";
    private static final String FILE_NAME = "user_info.txt";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Firebase Notification Service")
                .setContentText("Listening for Firebase messages")
                .setSmallIcon(R.drawable.alert_icon)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);

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
        // Check if message contains a data payload.
        if (!remoteMessage.getData().isEmpty()) {

            String messageTitle = remoteMessage.getData().get("title");
            String messageBody = remoteMessage.getData().get("body");

            String[] lines = messageBody.split("\\n");
            ArrayList<String> lastKnownPositions = new ArrayList<>(Arrays.asList(lines));
            lastKnownPositions.remove(0);

            sendNotification(messageTitle, messageBody, lastKnownPositions);
        }
    }


    private void sendNotification(String title, String body, ArrayList<String> lastKnownPositions) {
        Intent intent = new Intent(this, EmergencyMapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putStringArrayListExtra("lastKnownPositions", lastKnownPositions);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.alert_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
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
