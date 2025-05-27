package com.huan.capture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huan.capture.sr.SRDemoActivity;

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
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (allPermissionsGranted()) {
            initialize();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        EsDevice device = new EsDevice();
        device.setDeviceName("扩展屏(SMARTISAN) 743");
        device.setDeviceIp("192.168.40.147");
        device.setDevicePort(5000);
        device.setFrom("com.huan.capture");
        device.setVersion(0);
        ConfigParams.mEsDevice = device;
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
            EsMessenger.get().stop();
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

        btnClient.setOnClickListener(view -> {
            if (ConfigParams.mEsDevice == null) {
                Toast.makeText(this, "请先选择投屏设备", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ClientActivity.class);
            startActivity(intent);
        });

        btnServer.setOnClickListener(view -> {
            if (ConfigParams.mEsDevice == null) {
                Toast.makeText(this, "请先选择投屏设备", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, SRDemoActivity.class);
            startActivity(intent);
        });

        btnSearch.setOnClickListener(view -> EsMessenger.get().search(this));

        Button btnScreen = findViewById(R.id.btnScreen);
        btnScreen.setOnClickListener(view -> {
            Intent intent = new Intent(this, ScreenActivity.class);
            startActivity(intent);
        });

        Button btnDeviceInfo = findViewById(R.id.btnDeviceInfo);
        btnDeviceInfo.setOnClickListener(view -> {
            EsCommand.CmdArgs args = new EsCommand.CmdArgs("")
                    .put("screenWidth", "1080")
                    .put("screenHeight", "1920")
                    .put("deviceName", "huan")
                    .put("deviceVersion", "1.0")
                    .put("osType", "Android");

            EsCommand cmd = EsCommand.makeCustomCommand("OnConfigParams")
                    .setEventData(args);
            EsMessenger.get().sendCommand(this, ConfigParams.mEsDevice, cmd);
        });

        Button btnDisconnect = findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(view -> {
            EsCommand.CmdArgs args = new EsCommand.CmdArgs("")
                    .put("status", "disconnect")
                    .put("message", "主动断开");

            EsCommand cmd = EsCommand.makeCustomCommand("OnDisconnect")
                    .setEventData(args);
            EsMessenger.get().sendCommand(this, ConfigParams.mEsDevice, cmd);
        });

        Button btnCompleted = findViewById(R.id.btnCompleted);
        btnCompleted.setOnClickListener(view -> {
            EsCommand.CmdArgs args = new EsCommand.CmdArgs("")
                    .put("status", "completed")
                    .put("message", "体态评估完成");

            EsCommand cmd = EsCommand.makeCustomCommand("OnCompleted")
                    .setEventData(args);
            EsMessenger.get().sendCommand(this, ConfigParams.mEsDevice, cmd);
        });

        EditText etWsIP = findViewById(R.id.etWsIP);
        Button btnWsTv = findViewById(R.id.btnWsTv);
        btnWsTv.setOnClickListener(view -> {
            Config.SOCKET_IP = etWsIP.getText().toString().trim();
            Intent intent = new Intent(this, TVActivity.class);
            startActivity(intent);
        });

        Button btnWsCamera = findViewById(R.id.btnWsCamera);
        btnWsCamera.setOnClickListener(view -> {
            Config.SOCKET_IP = etWsIP.getText().toString().trim();
            Intent intent = new Intent(this, OldClientActivity.class);
            startActivity(intent);
        });

        Button btnWsScreen = findViewById(R.id.btnWsScreen);
        btnWsScreen.setOnClickListener(view -> {
            Config.SOCKET_IP = etWsIP.getText().toString().trim();
            Intent intent = new Intent(this, OldClientActivityV2.class);
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
