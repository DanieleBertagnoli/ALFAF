package com.project.alfaf.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.Toast;

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
import com.project.alfaf.enums.NotificationMethodEnum;

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
        List<Long> contactIds = new ArrayList<>();
        List<String> contactNumbers = new ArrayList<>();
        Long firstContactId = null;
        String firstContactNumber = null;

        try (FileInputStream fis = openFileInput("contacts.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    Long contactId = Long.parseLong(parts[1]);
                    String contactNumber = getContactNumber(this, contactId);

                    contactIds.add(contactId);
                    contactNumbers.add(contactNumber);

                    if (firstContactId == null) {
                        firstContactId = contactId;
                        firstContactNumber = contactNumber;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check notification settings
        boolean callNotificationEnabled = isNotificationMethodEnabled(NotificationMethodEnum.CALL, this);
        boolean smsNotificationEnabled = isNotificationMethodEnabled(NotificationMethodEnum.SMS, this);
        boolean notificationEnabled = isNotificationMethodEnabled(NotificationMethodEnum.NOTIFICATION, this);

        // Call the first contact if call notification is enabled
        if (callNotificationEnabled && firstContactNumber != null) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + firstContactNumber));
            startActivity(callIntent);
        }

        // Notify all contacts if any notification method is enabled
        for (String contactNumber : contactNumbers) {
            // Send SMS if SMS notification is enabled
            if (smsNotificationEnabled) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    sendSms(contactNumber);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 100);
                }
            }
        }

        // Send app notification if enabled
        if (notificationEnabled) {
            sendEmergencyNotification(this, "emergency");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            // Retrieve the first contact from the list
            Long contactId = null;
            try (FileInputStream fis = openFileInput("contacts.txt");
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader br = new BufferedReader(isr)) {
                String line;
                if ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        contactId = Long.parseLong(parts[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Get the contact number using the contact name or ID
            String contactNumber = getContactNumber(this, contactId);
            sendSms(contactNumber);
        } else {
            Toast.makeText(this, "PERMISSION DENIED!", Toast.LENGTH_LONG).show();
        }
    }

    private static boolean isNotificationMethodEnabled(NotificationMethodEnum notificationMethodEnum, Context context) {
        boolean isEnabled = false;
        try (FileInputStream fis = context.openFileInput("notification_settings.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(notificationMethodEnum + ": ")) {
                    isEnabled = Boolean.parseBoolean(line.split(": ")[1]);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isEnabled;
    }

    public static void sendEmergencyNotification(Context context, String emergencyType) {

        if(!isNotificationMethodEnabled(NotificationMethodEnum.NOTIFICATION, context)){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String phoneNumber = getPhoneNumber(context);
                    List<String> contacts = getContacts(context);
                    ArrayList<String> lastKnownPosition = readLastKnownPosition(context);
                    StringBuilder messageBuilder = new StringBuilder(emergencyType+ " " + phoneNumber + " ");
                    for (String position : lastKnownPosition) {
                        messageBuilder.append(position.replaceAll("\\s","_")).append("_");
                    }
                    for (String contact : contacts) {
                        messageBuilder.append(" ").append(contact);
                    }
                    String message = messageBuilder.toString();

                    URL url = new URL("http://100.104.220.21:5000");
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
                String[] parts = line.split(",");
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
        // Read user information from user_info.txt
        String userInfo = readUserInfo();
        // Read the last known position from position_log.txt
        ArrayList<String> lastKnownPosition = readLastKnownPosition(this);

        // Craft the SMS message
        String message = "Hi, this message was generated by the ALFAF application. " + userInfo +
                " could be in a possible emergency. These are the last known coordinates: ";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(contactNumber, null, message, null, null);
            for (String s: lastKnownPosition) {
                smsManager.sendTextMessage(contactNumber, null, s, null, null);
            }
            Toast.makeText(getApplicationContext(),"Message Sent to " + contactNumber,Toast.LENGTH_LONG).show();
        }catch (Exception e)
        {
            Toast.makeText(getApplicationContext(),"Some fields are empty",Toast.LENGTH_LONG).show();
        }
    }

    private String readUserInfo() {
        StringBuilder userInfo = new StringBuilder();
        try (FileInputStream fis = openFileInput("user_info.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                userInfo.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userInfo.toString();
    }

    private static ArrayList<String> readLastKnownPosition(Context context) {
        ArrayList<String> lastKnownPosition = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput("position_log.txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                lastKnownPosition.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lastKnownPosition;
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
