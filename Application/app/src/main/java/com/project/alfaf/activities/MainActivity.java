package com.project.alfaf.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.project.alfaf.R;
import com.project.alfaf.enums.DetectionsEnum;

import com.project.alfaf.services.FallDetectionService;
import com.project.alfaf.services.FirebaseNotificationService;
import com.project.alfaf.services.PositionLoggingService;
import com.project.alfaf.services.ShakeDetectionService;
import com.project.alfaf.utils.MyApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String USER_INFO_FILE_NAME = "user_info.txt";
    private static final String DETECTION_SETTINGS_FILE_NAME = "detection_settings.txt";
    private static final String TAG = "MainActivity";

    private boolean isFallDetectionEnabled = false;
    private boolean isShakeDetectionEnabled = false;
    private boolean isFightDetectionEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check if user info file exists
        if (!userInfoFileExists()) {
            Intent signupIntent = new Intent(this, SignupActivity.class);
            startActivity(signupIntent);
            finish();
            return;
        }
        else{
            Intent serviceIntent = new Intent(this, FirebaseNotificationService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }

        // Load detection settings
        loadDetectionSettings();

        // Request necessary permissions
        requestNecessaryPermissions();

        // Add event listener to emergency button
        MaterialButton emergencyBtn = findViewById(R.id.emergency_btn);
        emergencyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmergencyModeActivity.class);
            startActivity(intent);
        });

        // Add event listener to settings button
        ImageButton settingsBtn = findViewById(R.id.settings_btn);
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class); // Assuming SettingsActivity
            startActivity(intent);
        });

        FirebaseNotificationService.sendRegistrationToServer(this);
    }

    private boolean userInfoFileExists() {
        File file = new File(getFilesDir(), USER_INFO_FILE_NAME);
        return file.exists();
    }

    private void startLoggingService() {
        Intent serviceIntent = new Intent(this, PositionLoggingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void startShakeDetectionService() {
        if (isShakeDetectionEnabled) {
            Intent serviceIntent = new Intent(this, ShakeDetectionService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }
    }

    private void startFallDetectionService() {
        if (isFallDetectionEnabled) {
            Intent serviceIntent = new Intent(this, FallDetectionService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }
    }

    private void startDetectionServices() {
        startShakeDetectionService();
        startFallDetectionService();
    }

    private void loadDetectionSettings() {
        File file = new File(getFilesDir(), DETECTION_SETTINGS_FILE_NAME);
        if (!file.exists()) {
            return;
        }

        try (FileInputStream fis = openFileInput(DETECTION_SETTINGS_FILE_NAME);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    String option = parts[0];
                    boolean isChecked = Boolean.parseBoolean(parts[1]);
                    switch (DetectionsEnum.valueOf(option)) {
                        case FALL_DETECTION:
                            isFallDetectionEnabled = isChecked;
                            break;
                        case SHAKE_DETECTION:
                            isShakeDetectionEnabled = isChecked;
                            break;
                        case FIGHT_DETECTION:
                            isFightDetectionEnabled = isChecked;
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestNecessaryPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            // Start services if all permissions are already granted
            startLoggingService();
            startDetectionServices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                startLoggingService();
                startDetectionServices();
            } else {
                // Handle the case where permissions are not granted
                // You can show a message to the user or disable certain features
            }
        }
    }
}
