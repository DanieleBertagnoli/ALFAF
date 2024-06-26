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
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.project.alfaf.activities.EmergencyModeActivity;
import com.project.alfaf.activities.MainActivity;
import com.project.alfaf.utils.MyApp;
import com.project.alfaf.R;
import com.project.alfaf.utils.NotificationUtil;

public class ShakeDetectionService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "ShakeDetectionChannel";
    private static final int NOTIFICATION_ID = 3;
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;
    private static final int TIMER_DELAY_MS = 10000;

    private SensorManager mSensorManager;
    private long mShakeTimestamp;
    private int mShakeCount;

    private Handler mHandler;
    private Runnable mTimerRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        NotificationUtil.createGeneralNotificationChannel(this);
        startForeground(NotificationUtil.NOTIFICATION_ID, NotificationUtil.getGeneralNotification(this, "Shake detection"));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        mHandler = new Handler(Looper.getMainLooper());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mTimerRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            final long now = System.currentTimeMillis();
            if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return;
            }

            if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                mShakeCount = 0;
            }

            mShakeTimestamp = now;
            mShakeCount++;

            if (mShakeCount > 0) {
                handleShakeEvent();
            }
        }
    }

    private void handleShakeEvent() {
        if (MyApp.isAppInForeground()) {
            Intent intent = new Intent(this, EmergencyModeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            sendShakeDetectedNotification();
        }
    }

    private void sendShakeDetectedNotification() {
        Intent intent = new Intent(this, EmergencyModeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Shake detected")
                .setContentText("Shake event detected by the service")
                .setSmallIcon(R.drawable.alert_icon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID + 1, notification);  // Ensure unique notification ID

        // Start the timer
        startTimer();
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Shake Detection Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.emergency);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            serviceChannel.setSound(soundUri, audioAttributes);

            long[] vibrate = { 0, 100, 200, 300 };
            serviceChannel.enableVibration(true);
            serviceChannel.enableLights(true);
            serviceChannel.setVibrationPattern(vibrate);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
