package com.project.alfaf;

import android.Manifest;
import android.content.Context;
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

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int CALL_PERMISSION_REQUEST_CODE = 1001;
    private static final String USER_INFO_FILE_NAME = "user_info.txt";

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

        // Request location permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLoggingService();
        }

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

        // Request call permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_REQUEST_CODE);
        }

        // Start shake detection service
        Intent serviceIntent = new Intent(this, ShakeDetectionService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private boolean userInfoFileExists() {
        File file = new File(getFilesDir(), USER_INFO_FILE_NAME);
        return file.exists();
    }

    private void startLoggingService() {
        Intent serviceIntent = new Intent(this, PositionLoggingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLoggingService();
            }
        }
    }
}
