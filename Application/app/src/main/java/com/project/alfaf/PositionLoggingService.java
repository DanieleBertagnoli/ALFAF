package com.project.alfaf;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PositionLoggingService extends Service {

    private static final String CHANNEL_ID = "PositionLoggingServiceChannel";
    private static final String LOG_FILE_NAME = "position_log.txt";
    private static final int MAX_POSITIONS = 5;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(120000); // Sleep for 2 minutes
                        logCurrentPosition();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(runnable).start();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Position Logging Service Channel";
            String description = "Foreground service channel for logging positions";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Position Logging Service")
                .setContentText("Running")
                .setSmallIcon(R.drawable.alert_icon)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void logCurrentPosition() {
        // Get current position and timestamp
        getCurrentPosition();
    }

    private void getCurrentPosition() {
        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are not granted, log or handle the situation
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(120000); // 2 minutes interval

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    String currentPosition = "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude();
                    String timestamp = getCurrentTimestamp();
                    savePositionToFile(currentPosition, timestamp);
                }
            }
        }, Looper.myLooper());
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    private void savePositionToFile(String position, String timestamp) {
        try {
            File file = new File(getFilesDir(), LOG_FILE_NAME);
            if (!file.exists()) {
                file.createNewFile();
            }

            // Read existing positions from file
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder positionsBuilder = new StringBuilder();
            String line;
            int positionCount = 0;
            while ((line = reader.readLine()) != null && positionCount < MAX_POSITIONS - 1) {
                positionsBuilder.append(line).append("\n");
                positionCount++;
            }
            reader.close();

            // Add new position to the beginning
            positionsBuilder.insert(0, position + " - " + timestamp + "\n");

            // Write positions back to file
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(positionsBuilder.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
