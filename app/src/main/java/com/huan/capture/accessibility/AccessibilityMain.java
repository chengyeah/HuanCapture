package com.huan.capture.accessibility;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huan.capture.R;

public class AccessibilityMain extends AppCompatActivity {
    public static final int ACCESSIBILITY_CODE = 1002;
    private int number = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessibility_main);

        findViewById(R.id.btn_start).setOnClickListener(view -> {
            if (!AuxiliaryService.isStart()) {
                Toast.makeText(this, "打开辅助功能", Toast.LENGTH_LONG).show();
                try {
                    startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), ACCESSIBILITY_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        TextView tvNumber = findViewById(R.id.tvNumber);
        findViewById(R.id.btn_install).setOnClickListener(view -> {
            number++;
            tvNumber.setText("点击了" + number + "次");
        });
    }
}
