package com.project.alfaf;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView backBtn = findViewById(R.id.back_btn_settings);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
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
    }
}
