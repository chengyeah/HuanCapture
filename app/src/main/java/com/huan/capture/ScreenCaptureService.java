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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.webrtc.*;

public class ScreenCaptureService extends Service {

    public static ScreenCaptureCallback callback;

    private static final String CHANNEL_ID = "screen_capture_channel";
    private static final int NOTIFICATION_ID = 1001;

    private VideoCapturer videoCapturer;
    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private VideoSource videoSource;
    private VideoTrack videoTrack;

    public static void setCallback(ScreenCaptureCallback cb) {
        callback = cb;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 EGL 和 PeerConnectionFactory（一次性）
        eglBase = EglBase.create();
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(
                        eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.hasExtra("data")) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // 必须启动前台服务，适配 Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        Intent data = intent.getParcelableExtra("data");
        videoCapturer = new ScreenCapturerAndroid(data, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopSelf();
            }
        });

        // 初始化 capturer
        SurfaceTextureHelper surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());

        videoSource = factory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());

        try {
            videoCapturer.startCapture(480, 640, 16);
        } catch (Exception e) {
            Log.e("ScreenCaptureService", "startCapture failed", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        videoTrack = factory.createVideoTrack("screenTrack", videoSource);

        // 回调通知 Activity
        if (callback != null) {
            callback.onScreenCaptureReady(videoTrack);
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
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer.dispose();
        }
        if (videoSource != null) videoSource.dispose();
        if (eglBase != null) eglBase.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
