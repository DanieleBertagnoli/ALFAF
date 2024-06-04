package com.project.alfaf.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.project.alfaf.activities.EmergencyModeActivity;
import com.project.alfaf.activities.MainActivity;
import com.project.alfaf.R;
import com.project.alfaf.utils.MyApp;
import com.project.alfaf.utils.NotificationUtil;

public class FallDetectionService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "FallDetectionServiceChannel";
    private static final int NOTIFICATION_ID = 1002;
    private static final float FALL_THRESHOLD = 2.0f; // Threshold value for free fall detection
    private static final int TIMER_DELAY_MS = 10000;
    private SensorManager sensorManager;
    private Handler mHandler;
    private Runnable mTimerRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        NotificationUtil.createGeneralNotificationChannel(this);
        startForeground(NotificationUtil.NOTIFICATION_ID, NotificationUtil.getGeneralNotification(this, "Fall detection"));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        mHandler = new Handler(Looper.getMainLooper());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void onDestroy() {
        sensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mTimerRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double acceleration = Math.sqrt(x * x + y * y + z * z);

            if (acceleration < FALL_THRESHOLD) {
                Log.d("FallDetectionService", "Phone is falling!");
                handleFallEvent();
            }
        }
    }

    private void startTimer() {
        mHandler.removeCallbacks(mTimerRunnable);
        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                EmergencyModeActivity.sendEmergencyNotification(getApplicationContext(), "possible_emergency");
            }
        };
        mHandler.postDelayed(mTimerRunnable, TIMER_DELAY_MS);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this example
    }

    private void handleFallEvent() {
        if (MyApp.isAppInForeground()) {
            Intent intent = new Intent(this, EmergencyModeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            sendFallAlertNotification();
            startTimer();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fall Detection Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void sendFallAlertNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fall Detected")
                .setContentText("Your phone appears to be falling!")
                .setSmallIcon(R.drawable.alert_icon)
                .build();

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID + 1, notification); // Ensure unique ID for the alert
        }
    }
}
