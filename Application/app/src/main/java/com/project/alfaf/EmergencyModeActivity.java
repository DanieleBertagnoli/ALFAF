package com.project.alfaf;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

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

    private void manageEmergency(){
        // TODO: add emergency management logic
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
