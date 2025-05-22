package com.huan.capture;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.webrtc.*;

public class ScreenCaptureService extends Service {
    public static ScreenCaptureCallback callback;
    private static final String CHANNEL_ID = "screen_capture_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static void setCallback(ScreenCaptureCallback cb) {
        callback = cb;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        Intent data = intent.getParcelableExtra("data");
        ScreenCapturerAndroid videoCapturer = new ScreenCapturerAndroid(data, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopSelf();
            }
        });
        if (callback != null) {
            callback.onScreenCaptureReady(videoCapturer);
        }

        return START_STICKY;
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Screen Capture", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("录屏中")
                .setContentText("屏幕录制服务正在运行")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
