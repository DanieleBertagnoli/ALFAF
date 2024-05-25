package com.project.alfaf;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.project.alfaf.enums.NotificationMethodEnum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class NotificationSettingsActivity extends AppCompatActivity {

    private CheckBox callNotification;
    private CheckBox appNotification;
    private CheckBox smsNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize CheckBoxes
        callNotification = findViewById(R.id.call_notification);
        appNotification = findViewById(R.id.app_notification);
        smsNotification = findViewById(R.id.sms_notification);

        // Read saved states and set checkboxes
        loadCheckboxStates();

        ImageView backBtn = findViewById(R.id.back_btn_notifications);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        ImageButton confirmBtn = findViewById(R.id.confirm_button_notification);
        confirmBtn.setOnClickListener(v -> {
            saveCheckboxStates();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void saveCheckboxStates() {
        boolean isCallNotificationChecked = callNotification.isChecked();
        boolean isAppNotificationChecked = appNotification.isChecked();
        boolean isSmsNotificationChecked = smsNotification.isChecked();

        String data = NotificationMethodEnum.CALL+ ": " + isCallNotificationChecked + "\n" +
                NotificationMethodEnum.SMS+ ": " + isSmsNotificationChecked + "\n" +
                NotificationMethodEnum.NOTIFICATION+ ": " + isAppNotificationChecked;

        FileOutputStream fos = null;
        try {
            fos = openFileOutput("notification_settings.txt", Context.MODE_PRIVATE);
            fos.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadCheckboxStates() {
        FileInputStream fis = null;
        try {
            File file = new File(getFilesDir(), "notification_settings.txt");
            fis = openFileInput("notification_settings.txt");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(NotificationMethodEnum.CALL+ ": ")) {
                    boolean isChecked = Boolean.parseBoolean(line.split(": ")[1]);
                    callNotification.setChecked(isChecked);
                } else if (line.contains(NotificationMethodEnum.NOTIFICATION+ ": ")) {
                    boolean isChecked = Boolean.parseBoolean(line.split(": ")[1]);
                    appNotification.setChecked(isChecked);
                } else if (line.contains(NotificationMethodEnum.SMS+ ": ")) {
                    boolean isChecked = Boolean.parseBoolean(line.split(": ")[1]);
                    smsNotification.setChecked(isChecked);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
