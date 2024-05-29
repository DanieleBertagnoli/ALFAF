package com.project.alfaf.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.project.alfaf.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class PositionLoggingService extends Service {

    private static final String CHANNEL_ID = "PositionLoggingServiceChannel";
    private static final String LOG_TAG = "PositionLoggingService";
    private static final String LOG_FILE_NAME = "position_log.txt";
    private static final int NOTIFICATION_ID = 2;  // Unique notification ID
    private static final int MAX_POSITIONS = 5;
    private LocationManager locationManager;
    private LinkedList<String> positions = new LinkedList<>();
    private Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable positionLogger = new Runnable() {
        @Override
        public void run() {
            logPosition();
            handler.postDelayed(this, 120000); // Schedule next execution
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        handler.post(positionLogger); // Start logging positions
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(positionLogger);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Position Logging Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Position Logging Active")
                .setContentText("Logging position every 2 minutes")
                .setSmallIcon(R.drawable.alert_icon)
                .build();
    }

    private void logPosition() {
        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    String timestamp = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(new Date());
                    String position = location.getLatitude() + " " + location.getLongitude() + " " + timestamp;
                    if (positions.size() >= MAX_POSITIONS) {
                        positions.removeFirst();
                    }
                    positions.add(position);
                    savePositionsToFile();
                }

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Location permission not granted.", e);
        }
    }

    private void savePositionsToFile() {
        File file = new File(getFilesDir(), LOG_FILE_NAME);
        try (FileWriter writer = new FileWriter(file, false)) {
            for (String position : positions) {
                writer.write(position + "\n");
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error writing to log file.", e);
        }
    }
}
