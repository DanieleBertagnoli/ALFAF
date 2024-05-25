package com.project.alfaf;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.project.alfaf.enums.DetectionsEnum;
import com.project.alfaf.enums.NotificationMethodEnum;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class SignupActivity extends AppCompatActivity {

    private static final String FILE_NAME = "user_info.txt";
    private static final String SERVER_URL = "http://100.75.230.21:5000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etName = findViewById(R.id.etName);
        EditText etPhoneNumber = findViewById(R.id.etPhoneNumber);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phoneNumber = etPhoneNumber.getText().toString().trim();

            if (!name.isEmpty() && !phoneNumber.isEmpty()) {
                saveUserInfo(name, phoneNumber);
                sendUserInfoToServer(name, phoneNumber);
            }
        });

        createNotificationSettingsFile();
        createDetectionSettingsFile();
    }

    private void createDetectionSettingsFile() {
        String data = DetectionsEnum.FALL_DETECTION + ": true\n" +
                DetectionsEnum.SHAKE_DETECTION + ": true\n" +
                DetectionsEnum.FIGHT_DETECTION + ": true";

        try (FileOutputStream fos = openFileOutput("detection_settings.txt", MODE_PRIVATE);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationSettingsFile() {
        String data = NotificationMethodEnum.CALL + ": true\n" +
                NotificationMethodEnum.SMS + ": true\n" +
                NotificationMethodEnum.NOTIFICATION + ": true";

        try (FileOutputStream fos = openFileOutput("notification_settings.txt", MODE_PRIVATE);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void saveUserInfo(String name, String phoneNumber) {
        String userInfo = name + " " + phoneNumber;

        try (FileOutputStream fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendUserInfoToServer(String name, String phoneNumber) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                String message = "new_user " + name + " " + phoneNumber;
                OutputStream os = conn.getOutputStream();
                os.write(message.getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i("SignupActivity", "User info sent successfully.");
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Log.e("SignupActivity", "Failed to send user info. Response code: " + responseCode);
                    runOnUiThread(() -> showAlert("The server is not reachable, try again."));
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showAlert("The server is not reachable, try again. \n\nError: " + e));
            }
        }).start();
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Server Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

}
