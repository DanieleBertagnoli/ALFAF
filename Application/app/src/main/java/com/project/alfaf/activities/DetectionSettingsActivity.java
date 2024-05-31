package com.project.alfaf.activities;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.project.alfaf.R;
import com.project.alfaf.enums.DetectionsEnum;
import com.project.alfaf.services.FallDetectionService;
import com.project.alfaf.services.ShakeDetectionService;
import com.project.alfaf.utils.NotificationUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class DetectionSettingsActivity extends AppCompatActivity {

    private SwitchMaterial fallDetectionSwitch;
    private SwitchMaterial fightDetectionSwitch;
    private SwitchMaterial shakeDetectionSwitch;

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
            finish();
            getOnBackPressedDispatcher().onBackPressed();
        });

        fallDetectionSwitch = findViewById(R.id.fall_detection_option);
        fightDetectionSwitch = findViewById(R.id.fight_detection_option);
        shakeDetectionSwitch = findViewById(R.id.shake_detection_option);

        // Load switch states from file
        loadSwitchStates();

        ImageButton confirmBtn = findViewById(R.id.confirm_button_detections);
        confirmBtn.setOnClickListener(v -> {
            saveSwitchStates();
            finish();
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void saveSwitchStates() {
        try (FileOutputStream fos = openFileOutput("detection_settings.txt", Context.MODE_PRIVATE)) {
            String data = DetectionsEnum.FALL_DETECTION + ": " + fallDetectionSwitch.isChecked() + "\n" +
                    DetectionsEnum.SHAKE_DETECTION + ": " + shakeDetectionSwitch.isChecked() + "\n" +
                    DetectionsEnum.FIGHT_DETECTION + ": " + fightDetectionSwitch.isChecked();
            fos.write(data.getBytes());

            // Check and stop/start services based on switch states
            if (fallDetectionSwitch.isChecked()) {
                if (!isServiceRunning(FallDetectionService.class)) {
                    startService(new Intent(this, FallDetectionService.class));
                }
            } else {
                stopService(new Intent(this, FallDetectionService.class));
                NotificationUtil.stopService(this, "Fall detection");
            }

            if (shakeDetectionSwitch.isChecked()) {
                if (!isServiceRunning(ShakeDetectionService.class)) {
                    startService(new Intent(this, ShakeDetectionService.class));
                }
            } else {
                stopService(new Intent(this, ShakeDetectionService.class));
                NotificationUtil.stopService(this, "Shake detection");
            }

            if (fightDetectionSwitch.isChecked()) {
                // Uncomment and modify the following line if you have a FightDetectionService
                // if (!isServiceRunning(FightDetectionService.class)) {
                //     startService(new Intent(this, FightDetectionService.class));
                // }
            } else {
                // Uncomment and modify the following line if you have a FightDetectionService
                // stopService(new Intent(this, FightDetectionService.class));
            }

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
                    switch (DetectionsEnum.valueOf(option)) {
                        case FALL_DETECTION:
                            fallDetectionSwitch.setChecked(isChecked);
                            break;
                        case SHAKE_DETECTION:
                            shakeDetectionSwitch.setChecked(isChecked);
                            break;
                        case FIGHT_DETECTION:
                            fightDetectionSwitch.setChecked(isChecked);
                            break;
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
