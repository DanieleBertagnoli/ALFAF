package com.project.alfaf;

import android.content.Context;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
        Long contactId = null;
        try (FileInputStream fis = openFileInput("contacts.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            if ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    contactId = Long.parseLong(parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the contact number using the contact name or ID
        String contactNumber = getContactNumber(this, contactId);

        // Check notification settings
        boolean callNotificationEnabled = isNotificationMethodEnabled(NotificationMethod.CALL, this);
        boolean smsNotificationEnabled = isNotificationMethodEnabled(NotificationMethod.SMS, this);
        boolean notificationEnabled = isNotificationMethodEnabled(NotificationMethod.NOTIFICATION, this);

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

        // Send SMS if SMS notification is enabled
        if (notificationEnabled) {
            sendEmergencyNotification(this);
        }
    }

    private static boolean isNotificationMethodEnabled(NotificationMethod notificationMethod, Context context) {
        boolean isEnabled = false;
        try (FileInputStream fis = context.openFileInput("notification_settings.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(notificationMethod + ": ")) {
                    isEnabled = Boolean.parseBoolean(line.split(": ")[1]);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isEnabled;
    }

    public static void sendEmergencyNotification(Context context) {

        if(!isNotificationMethodEnabled(NotificationMethod.NOTIFICATION, context)){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String phoneNumber = getPhoneNumber(context);
                    List<String> contacts = getContacts(context);
                    StringBuilder messageBuilder = new StringBuilder("emergency " + phoneNumber);
                    for (String contact : contacts) {
                        messageBuilder.append(" ").append(contact);
                    }
                    String message = messageBuilder.toString();

                    URL url = new URL("http://100.75.230.21:5000");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                    OutputStream os = conn.getOutputStream();
                    os.write(message.getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        System.out.println("Success");
                    } else {
                        System.out.println(responseCode);
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static String getPhoneNumber(Context context) {
        String phoneNumber = "";
        try {
            FileInputStream fis = context.openFileInput("user_info.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            if ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    phoneNumber = parts[1];
                }
            }
            reader.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return phoneNumber;
    }

    private static List<String> getContacts(Context context) {
        List<String> contacts = new ArrayList<>();

        try (FileInputStream fis = context.openFileInput("contacts.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    long contactId = Long.parseLong(parts[1]);
                    String contactNumber = getContactNumber(context, contactId);
                    if (!contactNumber.isEmpty()) {
                        contacts.add(contactNumber);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contacts;
    }


    private void sendSms(String contactNumber) {
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("smsto:" + contactNumber));
        smsIntent.putExtra("sms_body", "Hi, this message was generated by the ALFAF application. NameOfThePerson could be in a possible emergency. These are the last known coordinates:");
        startActivity(smsIntent);
    }

    private static String getContactNumber(Context context, Long contactId) {
        String contactNumber = null;
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{String.valueOf(contactId)}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if (columnIndex >= 0) {
                contactNumber = cursor.getString(columnIndex);
                contactNumber = contactNumber.replace(" ", "");
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
