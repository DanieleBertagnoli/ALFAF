package com.project.alfaf;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class EmergencyModeActivity extends AppCompatActivity {

    private final int automaticConfirmTimer = 10; // Automatic confirm timer in seconds
    private CountDownTimer countDownTimer;
    private TextView autoActivateTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency_mode);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Adding confirm button listener
        RelativeLayout confirmBtn = findViewById(R.id.confirm_btn);
        confirmBtn.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            manageEmergency();
        });

        // Adding revert button listener
        MaterialButton revertBtn = findViewById(R.id.revert_btn);
        revertBtn.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        autoActivateTxt = findViewById(R.id.auto_activate_txt);

        // Start the countdown timer
        startConfirmCountdown();
    }

    private void startConfirmCountdown() {
        countDownTimer = new CountDownTimer(automaticConfirmTimer * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String newText = "AUTOMATIC CONFIRM IN " + millisUntilFinished/1000 + "s";
                autoActivateTxt.setText(newText);
            }

            @Override
            public void onFinish() {
                manageEmergency();
            }
        }.start();
    }

    private void manageEmergency() {
        // Retrieve the first contact from the list
        String contactName = null;
        Long contactId = null;
        try (FileInputStream fis = openFileInput("contacts.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            if ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    contactName = parts[0];
                    contactId = Long.parseLong(parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the contact number using the contact name or ID
        String contactNumber = getContactNumber(contactId);

        // Check notification settings
        boolean callNotificationEnabled = isCallNotificationEnabled();
        boolean smsNotificationEnabled = isSmsNotificationEnabled();

        // Call the contact if call notification is enabled
        if (callNotificationEnabled) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + contactNumber));
            startActivity(callIntent);
        }

        // Send SMS if SMS notification is enabled
        if (smsNotificationEnabled) {
            sendSms(contactNumber);
        }
    }

    private boolean isCallNotificationEnabled() {
        boolean isEnabled = false;
        try (FileInputStream fis = openFileInput("notification_settings.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Call Notification: ")) {
                    isEnabled = Boolean.parseBoolean(line.split(": ")[1]);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isEnabled;
    }

    private boolean isSmsNotificationEnabled() {
        boolean isEnabled = false;
        try (FileInputStream fis = openFileInput("notification_settings.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("SMS Notification: ")) {
                    isEnabled = Boolean.parseBoolean(line.split(": ")[1]);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isEnabled;
    }

    private void sendSms(String contactNumber) {
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("smsto:" + contactNumber));
        smsIntent.putExtra("sms_body", "Hi, this message was generated by the ALFAF application. NameOfThePerson could be in a possible emergency. These are the last known coordinates:");
        startActivity(smsIntent);
    }

    private String getContactNumber(Long contactId) {
        String contactNumber = null;
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{String.valueOf(contactId)}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if (columnIndex >= 0) {
                contactNumber = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return contactNumber;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
