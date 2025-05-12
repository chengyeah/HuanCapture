package com.huan.capture;

import static org.webrtc.SessionDescription.Type.ANSWER;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import eskit.sdk.support.messenger.client.EsMessenger;
import eskit.sdk.support.messenger.client.bean.EsEvent;
import eskit.sdk.support.messenger.client.core.EsCommand;

public class ScreenActivity extends AppCompatActivity {
    private final MediaConstraints mediaConstraints = new MediaConstraints();

    private boolean isFrontCamera = true; // 默认使用前置摄像头
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnectionLocal;
    private SurfaceViewRenderer localView;
    private MediaStream mediaStreamLocal;
    private EglBase eglBase;
    private boolean isMirror = false;
    private VideoCapturer videoCapturer;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private MediaProjectionManager projectionManager;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private Intent serviceIntent;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);

        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("MaxBitrateBps", "800_000")); // 800kbps

        ConfigParams.getInstance().setOnClientMessageListener(new ConfigParams.OnClientMessageListener() {
            @Override
            public void onMessage(EsEvent esEvent) {
                Log.i("--==>", "onMessage: " + esEvent.toString());
                Gson gson = new Gson();
                ActionBean actionBean = gson.fromJson(esEvent.getData(), ActionBean.class);
                switch (actionBean.getAction()) {
                    case "answer":
                        Log.i("--==>", "onMessage------>: " + actionBean.getArgs());
                        ActionBean.ActionBeanTDO actionBeanTDO = gson.fromJson(actionBean.getArgs(), ActionBean.ActionBeanTDO.class);
                        Log.i("--==>", "onMessage------====>: " + actionBeanTDO.getSdp());
                        handleAnswer(new SessionDescription(ANSWER, actionBeanTDO.getSdp()));
                        break;
                }
            }
        });

        eglBase = EglBase.create();
        eglBase.createDummyPbufferSurface();
        eglBase.makeCurrent();

        // 第一步：创建PeerConnectionFactory
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                .builder(this)
                .createInitializationOptions());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = 0;
        DefaultVideoEncoderFactory defaultVideoEncoderFactory =
                new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory =
                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

//        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());

        // 创建 VideoCapturer
//        videoCapturer = createCameraCapturer(true);
//        // 用PeerConnectionFactory创建VideoSource
//        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
//        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
//        videoCapturer.startCapture(320, 240, 15);

        localView = findViewById(R.id.localView);
        localView.setMirror(false);
        localView.init(eglBase.getEglBaseContext(), null);

        // 用PeerConnectionFactory和VideoSource创建VideoTrack
//        videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
//        videoTrack.addSink(localView);

//        mediaStreamLocal = peerConnectionFactory.createLocalMediaStream("mediaStreamLocal");
//        mediaStreamLocal.addTrack(videoTrack);

        Button btnCall = findViewById(R.id.btnCall);
        btnCall.setOnClickListener(view -> {
            call(mediaStreamLocal);
        });

        Button btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnSwitchCamera.setOnClickListener(view -> {
            projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 设置回调
                ScreenCaptureService.setCallback(videoTrack -> runOnUiThread(() -> {
                    videoTrack.addSink(localView);

                    mediaStreamLocal = peerConnectionFactory.createLocalMediaStream("mediaStreamLocal");
                    mediaStreamLocal.addTrack(videoTrack);

                    if (peerConnectionLocal != null) {
                        try {
                            peerConnectionLocal.addTrack(videoTrack);
                        } catch (Exception e) {
                            Log.e("ScreenActivity", "添加屏幕轨道失败", e);
                        }
                    }
                }));

                // Android 10+：必须启动前台服务
                serviceIntent = new Intent(this, ScreenCaptureService.class);
                serviceIntent.putExtra("data", data); // 把 MediaProjection 的授权数据传进去
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                // Android 9 及以下：直接初始化录屏
                initScreenCapture(data);
            }
        }
    }

    private void initScreenCapture(Intent data) {
        videoCapturer = new ScreenCapturerAndroid(data, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d("ScreenCapture", "录屏已停止");
            }
        });

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(480, 640, 16);

        videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
        videoTrack.addSink(localView);

        mediaStreamLocal = peerConnectionFactory.createLocalMediaStream("mediaStreamLocal");
        mediaStreamLocal.addTrack(videoTrack);

        if (peerConnectionLocal != null) {
            try {
                peerConnectionLocal.addTrack(videoTrack);
            } catch (Exception e) {
                Log.e("ScreenActivity", "添加屏幕轨道失败", e);
            }
        }
    }

    private void call(MediaStream localMediaStream) {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        peerConnectionLocal = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnectionAdapter("localconnection") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Log.i("--==>", "接收到onIceCandidate回调 is：" + iceCandidate.toString());
                // 发送 ICE 到 TV 端
                sendIceCandidateToTV(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                // 不需要处理远程流
            }
        });

        // 只向 peerConnectionLocal 添加本地媒体流
        peerConnectionLocal.addStream(localMediaStream);
        peerConnectionLocal.createOffer(new SdpAdapter("local offer sdp") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnectionLocal.setLocalDescription(new SdpAdapter("local set local"), sessionDescription);
                // 发送 offer 到 TV 端
                sendOfferToTV(sessionDescription);
            }
        }, mediaConstraints);
    }

    private void handleAnswer(SessionDescription sessionDescription) {
        peerConnectionLocal.setRemoteDescription(new SdpAdapter("local set remote"), sessionDescription);
    }

    private void sendIceCandidateToTV(IceCandidate iceCandidate) {
        EsCommand.CmdArgs args = new EsCommand.CmdArgs("home")
                .put("action", "candidate")
                .put("sdpMid", iceCandidate.sdpMid)
                .put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
                .put("candidate", iceCandidate.sdp);
        EsCommand cmd = EsCommand.makeCustomCommand("OnLinkWebRTC")
                .setEventData(args);

        EsMessenger.get().sendCommand(this, ConfigParams.mEsDevice, cmd);
    }

    private void sendOfferToTV(SessionDescription sessionDescription) {
        String sdpFormat = sessionDescription.description.replaceAll("\r\n", "#");
        Log.i("--==>", "原始offer数据 : " + sessionDescription.description);
        Log.i("--==>", "处理后offer数据 : " + sdpFormat);
        // 分片发送
        int maxLength = 500;
        int totalChunks = (int) Math.ceil((double) sdpFormat.length() / maxLength);

        for (int i = 0; i < sdpFormat.length(); i += maxLength) {
            int end = Math.min(i + maxLength, sdpFormat.length());
            String chunk = sdpFormat.substring(i, end);
            int chunkNumber = i / maxLength + 1;

            EsCommand.CmdArgs args = new EsCommand.CmdArgs("home")
                    .put("action", "offer")
                    .put("sdp", chunk)
                    .put("chunkNumber", chunkNumber)
                    .put("totalChunks", totalChunks);
            EsCommand cmd = EsCommand.makeCustomCommand("OnLinkWebRTC")
                    .setEventData(args);

            EsMessenger.get().sendCommand(this, ConfigParams.mEsDevice, cmd);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (peerConnectionLocal != null) {
            peerConnectionLocal.dispose();
            peerConnectionLocal = null;
        }

        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }

        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }

        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }

        if (videoTrack != null) {
            try {
                videoTrack.dispose(); // 安全调用
            } catch (Exception ignored) {
            }
            videoTrack = null;
        }

        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }

        if (localView != null) {
            localView.release();
            localView = null;
        }

        if (serviceIntent != null) {
            stopService(serviceIntent);
        }
    }
}
