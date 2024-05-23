package com.project.alfaf;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class DetectionSettingsActivity extends AppCompatActivity {

    private SwitchMaterial fallDetectionSwitch;
    private SwitchMaterial fightDetectionSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detection_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView backBtn = findViewById(R.id.back_btn_detections);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        fallDetectionSwitch = findViewById(R.id.fall_detection_option);
        fightDetectionSwitch = findViewById(R.id.fight_detection_option);

        // Load switch states from file
        loadSwitchStates();

        ImageButton confirmBtn = findViewById(R.id.confirm_button_detections);
        confirmBtn.setOnClickListener(v -> {
            saveSwitchStates();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void saveSwitchStates() {
        try (FileOutputStream fos = openFileOutput("detection_settings.txt", Context.MODE_PRIVATE)) {
            String data = "Fall Detection: " + fallDetectionSwitch.isChecked() + "\n" +
                    "Fight Detection: " + fightDetectionSwitch.isChecked();
            fos.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSwitchStates() {
        try (FileInputStream fis = openFileInput("detection_settings.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    String option = parts[0];
                    boolean isChecked = Boolean.parseBoolean(parts[1]);
                    switch (option) {
                        case "Fall Detection":
                            fallDetectionSwitch.setChecked(isChecked);
                            break;
                        case "Fight Detection":
                            fightDetectionSwitch.setChecked(isChecked);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
