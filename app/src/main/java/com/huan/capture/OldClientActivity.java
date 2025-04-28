package com.huan.capture;

import android.os.Bundle;
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
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class OldClientActivity extends AppCompatActivity {
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnectionLocal;
    private SurfaceViewRenderer localView;
    private MediaStream mediaStreamLocal;
    private EglBase eglBase;
    private WebSocketClientManager webSocketManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        webSocketManager = new WebSocketClientManager(this);
        webSocketManager.connectToServer("ws://192.168.80.9:38383");
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

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        // 创建 VideoCapturer
        VideoCapturer videoCapturer = createCameraCapturer(true);
        // 用PeerConnectionFactory创建VideoSource
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(480, 640, 30);

        localView = findViewById(R.id.localView);
        localView.setMirror(true);
        localView.init(eglBase.getEglBaseContext(), null);

        // 用PeerConnectionFactory和VideoSource创建VideoTrack
        VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
        videoTrack.addSink(localView);

        mediaStreamLocal = peerConnectionFactory.createLocalMediaStream("mediaStreamLocal");
        mediaStreamLocal.addTrack(videoTrack);

        Button btnCall = findViewById(R.id.btnCall);
        btnCall.setOnClickListener(view -> {
            call(mediaStreamLocal);
        });
    }

    private void call(MediaStream localMediaStream) {
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
        peerConnectionLocal.addStream(localMediaStream);
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

    private VideoCapturer createCameraCapturer(boolean isFront) {
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        final String[] deviceNames = enumerator.getDeviceNames();

        // 首先，试着找到前置摄像头
        for (String deviceName : deviceNames) {
            if (isFront ? enumerator.isFrontFacing(deviceName) : enumerator.isBackFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }
}

