package com.project.alfaf;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;


public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_list);

        ImageView backBtnSetting = findViewById(R.id.back_btn_setting);
        backBtnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to MainActivity
                Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                startActivity(intent);
                // Optionally, finish the current activity
                finish();
            }
        });
    }
}
