package com.project.alfaf.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.project.alfaf.R;
import com.project.alfaf.activities.EmergencyMapActivity;
import com.project.alfaf.activities.MainActivity;
import com.project.alfaf.enums.NotificationMethodEnum;
import com.project.alfaf.utils.NotificationUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class FirebaseNotificationService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseNotificationService";
    private static final String CHANNEL_ID = "FirebaseNotificationChannel";
    private static final String SERVER_URL = "http://100.104.220.21:5000";
    private static final String FILE_NAME = "user_info.txt";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TOKEN_KEY = "firebase_token";
    private static final String PREFS_NAME = "com.project.alfaf.PREFERENCE_FILE_KEY";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        NotificationUtil.createGeneralNotificationChannel(this);

        Notification notification = NotificationUtil.getGeneralNotification(this, "Firebase");
        startForeground(NotificationUtil.NOTIFICATION_ID, notification);
    }


    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        saveTokenToPreferences(token);
        sendRegistrationToServer(this);
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
        if (!isNotificationEnabled()) {
            Log.d(TAG, "Notifications are disabled in settings.");
            return;
        }

        Intent intent = new Intent(this, EmergencyMapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putStringArrayListExtra("lastKnownPositions", lastKnownPositions);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.alert_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Add this to dismiss the notification after it's clicked

        Notification notification = notificationBuilder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private boolean isNotificationEnabled() {
        FileInputStream fis = null;
        try {
            File file = new File(getFilesDir(), "notification_settings.txt");
            if (!file.exists()) {
                return false; // Default to false if the file doesn't exist
            }
            fis = openFileInput("notification_settings.txt");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(NotificationMethodEnum.NOTIFICATION + ": ")) {
                    return Boolean.parseBoolean(line.split(": ")[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false; // Default to false if there's an error
    }

    public static void sendRegistrationToServer(Context context) {
        new Thread(() -> {
            try {
                String token = getTokenFromPreferences(context);
                String phoneNumber = getPhoneNumberFromFile(context);

                if (phoneNumber != null && token != null) {
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
                    Log.e(TAG, "Phone number or token is null, could not send registration to server.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending registration to server: " + e.getMessage(), e);
            }
        }).start();
    }

    private static String getPhoneNumberFromFile(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
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
                Log.e(TAG, "Error reading phone number from file: " + e.getMessage(), e);
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
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void saveTokenToPreferences(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TOKEN_KEY, token);
        editor.apply();
    }

    private static String getTokenFromPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(TOKEN_KEY, null);
    }
}
