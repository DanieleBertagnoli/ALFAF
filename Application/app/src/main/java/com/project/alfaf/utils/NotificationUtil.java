package com.project.alfaf.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.project.alfaf.R;
import com.project.alfaf.activities.MainActivity;

import java.util.HashSet;

public class NotificationUtil {
    private static final String GENERAL_CHANNEL_ID = "GeneralServiceChannel";
    public static final int NOTIFICATION_ID = 9999;
    private static HashSet<String> runningServices;
    private static Notification notification;

    public static void createGeneralNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel existingChannel = manager.getNotificationChannel(GENERAL_CHANNEL_ID);
                if (existingChannel == null) {

                    runningServices = new HashSet<>();
                    notification = null;

                    NotificationChannel channel = new NotificationChannel(
                            GENERAL_CHANNEL_ID,
                            "General Service Channel",
                            NotificationManager.IMPORTANCE_LOW
                    );
                    channel.setDescription("Channel for general service notifications");
                    manager.createNotificationChannel(channel);
                }
            }
        }
    }

    public static void stopService(Context context, String service){
        runningServices.remove(service);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            Notification notification = getGeneralNotification(context, "");
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    public static Notification getGeneralNotification(Context context, String service) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        if (!service.isEmpty()){
            runningServices.add(service);
        }

        String allText = "";
        for(String t : runningServices){
            allText += t + ", ";
        }

        allText += " running in background";

        if(notification != null){
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(NOTIFICATION_ID);
                notification = null; // Clear the reference to the existing notification
            }
        }

        notification = new NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
                .setContentTitle("Service Running")
                .setContentText(allText)
                .setSmallIcon(R.drawable.alert_icon)
                .setContentIntent(pendingIntent)
                .build();

        return notification;
    }
}
