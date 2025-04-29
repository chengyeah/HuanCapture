package com.huan.capture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import eskit.sdk.support.messenger.client.EsMessenger;
import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.core.EsCommand;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private final List<EsDevice> mList = new ArrayList<>();
    private DeviceAdapter deviceAdapter;
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (allPermissionsGranted()) {
            initialize();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void initialize() {
        ConfigParams.getInstance().setOnClientMessageListener(new ConfigParams.OnClientMessageListener() {
            @Override
            public void onDeviceInfo(EsDevice esDevice) {
                mList.add(esDevice);
                runOnUiThread(() -> {
                    if (deviceAdapter != null) {
                        deviceAdapter.setData(mList);
                    }
                });
            }
        });
        TextView tvDeviceName = findViewById(R.id.tvDeviceName);
        RecyclerView rlvBox = findViewById(R.id.rlvBox);
        rlvBox.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(this);
        deviceAdapter.setOnItemClickListener(device -> {
            ConfigParams.mEsDevice = device;
            tvDeviceName.setText("已选择设备：" + device.getDeviceName() + "(" + device.getDeviceIp() + ":" + device.getDevicePort() + ")");
            Toast.makeText(SplashActivity.this, "已选择设备：" + device.getDeviceName(), Toast.LENGTH_SHORT).show();

            //TODO 联调本地使用
//            EsCommand cmd = EsCommand.makeEsAppCommand("debug")
//                    .put("uri", "192.168.40.80:38989")
//                    .setEventData(
//                            new EsCommand.CmdArgs("home")
//                                    .put("url", "https://hub.quicktvui.com/repository/public-files/video/dev/mp4/4.0/mp4-4.0.mp4")
//                    );
//
//            EsMessenger.get().sendCommand(SplashActivity.this, device, cmd);
        });

        rlvBox.setAdapter(deviceAdapter);

        Button btnClient = findViewById(R.id.btnClient);
        Button btnServer = findViewById(R.id.btnServer);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnSend = findViewById(R.id.btnSend);

        btnClient.setOnClickListener(view -> {
            if (ConfigParams.mEsDevice == null) {
                Toast.makeText(this, "请先选择投屏设备", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ClientActivity.class);
            startActivity(intent);
        });

        btnServer.setOnClickListener(view -> {
            Intent intent = new Intent(this, TVActivity.class);
            startActivity(intent);
        });

        btnSearch.setOnClickListener(view -> EsMessenger.get().search(this));

        btnSend.setOnClickListener(view -> {
            Intent intent = new Intent(this, OldClientActivity.class);
            startActivity(intent);
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initialize();
            } else {
                Toast.makeText(this, "权限被拒绝，无法继续操作", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
