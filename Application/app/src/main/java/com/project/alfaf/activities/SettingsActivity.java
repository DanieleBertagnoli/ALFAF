package com.project.alfaf.activities;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.project.alfaf.R;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView backBtn = findViewById(R.id.back_btn_settings);
        backBtn.setOnClickListener(v -> {
            finish();
            getOnBackPressedDispatcher().onBackPressed();
        });

        LinearLayout contactsBtn = findViewById(R.id.contacts_settings);
        contactsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ContactsSettingsActivity.class);
            startActivity(intent);
        });
        LinearLayout notificationBtn = findViewById(R.id.notification_settings);
        notificationBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationSettingsActivity.class);
            startActivity(intent);
        });
        LinearLayout detectionsBtn = findViewById(R.id.detections_settings);
        detectionsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetectionSettingsActivity.class);
            startActivity(intent);
        });

        // Inside onCreate() method
        Button resetBtn = findViewById(R.id.btn_reset);
        resetBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm Reset");
            builder.setMessage("Are you sure you want to reset all settings?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                // User clicked Yes button
                // Reset the settings here
                SignupActivity.createNotificationSettingsFile(getApplicationContext());
                SignupActivity.createDetectionSettingsFile(getApplicationContext());
                SignupActivity.createContactSettingsFile(getApplicationContext());
                Intent intent = new Intent(this, SignupActivity.class);
                startActivity(intent);
                Toast.makeText(this, "All settings have been reset!", Toast.LENGTH_LONG).show();
            });
            builder.setNegativeButton("No", (dialog, which) -> {
                // User clicked No button
                // Do nothing, just dismiss the dialog
                dialog.dismiss();
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }
}
