package com.huan.capture;

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

/**
 * socket  + WebRTC + 屏幕共享
 */
public class OldClientActivityV2 extends AppCompatActivity {
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnectionLocal;
    private SurfaceViewRenderer localView;
    private MediaStream mediaStreamLocal;
    private EglBase eglBase;
    private WebSocketClientManager webSocketManager;
    private VideoSource videoSource;
    private MediaProjectionManager projectionManager;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private VideoCapturer videoCapturer;
    private VideoTrack videoTrack;
    private Intent serviceIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        webSocketManager = new WebSocketClientManager(this);
        webSocketManager.connectToServer("ws://" + Config.SOCKET_IP + ":38383");
        webSocketManager.setOnAnswerReceivedListener(this::handleAnswer);
        webSocketManager.setOnIceCandidateReceivedListener(this::handleIceCandidate);

        eglBase = EglBase.create();
        eglBase.createDummyPbufferSurface();
        eglBase.makeCurrent();

        // 第一步：创建PeerConnectionFactory
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                .builder(this)
                .createInitializationOptions());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory =
                new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory =
                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        localView = findViewById(R.id.localView);
        localView.setMirror(true);
        localView.init(eglBase.getEglBaseContext(), null);


        Button btnCall = findViewById(R.id.btnCall);
        btnCall.setOnClickListener(view -> {
            call();
        });

        Button btnFlipCamera = findViewById(R.id.btnFlipCamera);
        btnFlipCamera.setOnClickListener(view -> {
            projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
        });
    }

    private void initScreenCapture() {
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(720, 1280, 15);
        videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
        videoSource.adaptOutputFormat(VideoSource.AspectRatio.UNDEFINED, null, VideoSource.AspectRatio.UNDEFINED, null, 15);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ScreenCaptureService.setCallback(_videoTrack -> runOnUiThread(() -> {
                    videoCapturer = _videoTrack;
                    initScreenCapture();
                }));

                serviceIntent = new Intent(this, ScreenCaptureService.class);
                serviceIntent.putExtra("data", data);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                videoCapturer = new ScreenCapturerAndroid(data, new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        super.onStop();
                        Log.d("ScreenCapture", "录屏已停止");
                    }
                });

                initScreenCapture();
            }
        }
    }

    private void call() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        peerConnectionLocal = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnectionAdapter("localconnection") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
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
//        peerConnectionLocal.addStream(localMediaStream);

        VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("video", videoSource);
        peerConnectionLocal.addTrack(videoTrack);

        peerConnectionLocal.createOffer(new SdpAdapter("local offer sdp") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnectionLocal.setLocalDescription(new SdpAdapter("local set local"), sessionDescription);
                // 发送 offer 到 TV 端
                sendOfferToTV(sessionDescription);
            }
        }, new MediaConstraints());
    }

    private void handleAnswer(SessionDescription sessionDescription) {
        peerConnectionLocal.setRemoteDescription(new SdpAdapter("local set remote"), sessionDescription);
    }

    private void handleIceCandidate(IceCandidate iceCandidate) {
        peerConnectionLocal.addIceCandidate(iceCandidate);
    }

    private void sendIceCandidateToTV(IceCandidate iceCandidate) {
        webSocketManager.sendIceCandidate(iceCandidate);
    }

    private void sendOfferToTV(SessionDescription sessionDescription) {
        webSocketManager.sendOffer(sessionDescription);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eglBase != null) {
            eglBase.release();
        }

        webSocketManager.disconnect();
    }
}

