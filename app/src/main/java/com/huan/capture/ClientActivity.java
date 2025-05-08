package com.huan.capture;

import static org.webrtc.SessionDescription.Type.ANSWER;

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

public class ClientActivity extends AppCompatActivity {
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        ConfigParams.getInstance().setOnClientMessageListener(new ConfigParams.OnClientMessageListener() {
            @Override
            public void onMessage(EsEvent esEvent) {
                Log.i("--==>", "onMessage: " + esEvent.toString());
                Gson gson = new Gson();
                ActionBean actionBean = gson.fromJson(esEvent.getData(), ActionBean.class);
                switch (actionBean.getAction()) {
                    case "answer":
//                        String args = actionBean.getArgs();
//                        Log.i("--==>", "onMessage------>: " + actionBean.getArgs());
//                        ActionBean.ActionBeanTDO actionBeanTDO = gson.fromJson(esEvent.getData(), ActionBean.ActionBeanTDO.class);
                        Log.i("--==>", "onMessage------>: " + actionBean.getArgs());
                        ActionBean.ActionBeanTDO actionBeanTDO = gson.fromJson(actionBean.getArgs(), ActionBean.ActionBeanTDO.class);
                        Log.i("--==>", "onMessage------====>: " + actionBeanTDO.getSdp());
                        handleAnswer(new SessionDescription(ANSWER, actionBeanTDO.getSdp()));
                        break;
                    case "switchCamera":
                        switchCamera();
                        break;
//                    case "candidate":
//                        handleIceCandidate(new IceCandidate(esEvent.getData().split(",")[0], Integer.parseInt(esEvent.getData().split(",")[1]), esEvent.getData().split(",")[2]));
//                        break;
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

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        // 创建 VideoCapturer
        videoCapturer = createCameraCapturer(true);
        // 用PeerConnectionFactory创建VideoSource
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(320, 240, 26);

        localView = findViewById(R.id.localView);
        localView.setMirror(false);
        localView.init(eglBase.getEglBaseContext(), null);

        // 用PeerConnectionFactory和VideoSource创建VideoTrack
        videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
        videoTrack.addSink(localView);

        mediaStreamLocal = peerConnectionFactory.createLocalMediaStream("mediaStreamLocal");
        mediaStreamLocal.addTrack(videoTrack);

        Button btnCall = findViewById(R.id.btnCall);
        btnCall.setOnClickListener(view -> {
            call(mediaStreamLocal);
        });

        //切换摄像头
        Button btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnSwitchCamera.setOnClickListener(view -> {
            switchCamera();
        });

        // 镜像
        Button btnFlipCamera = findViewById(R.id.btnFlipCamera);
        btnFlipCamera.setOnClickListener(view -> {
            isMirror = !isMirror;
            localView.setMirror(isMirror);

        });
    }

    private void switchCamera() {
        // 移除并释放当前的 MediaStreamTrack
        if (!mediaStreamLocal.videoTracks.isEmpty()) {
            VideoTrack oldVideoTrack = mediaStreamLocal.videoTracks.get(0);
            mediaStreamLocal.removeTrack(oldVideoTrack);
            oldVideoTrack.dispose();
        }

        try {
            videoCapturer.stopCapture();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            Log.e("ClientActivity", "Failed to stop capture", e);
            return;
        }

        videoCapturer.dispose();
        videoSource.dispose();

        isFrontCamera = !isFrontCamera;
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoCapturer = createCameraCapturer(isFrontCamera);
        if (videoCapturer == null) {
            Log.e("ClientActivity", "Failed to create camera capturer");
            return;
        }

        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(320, 240, 26);

        videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
        videoTrack.addSink(localView);

        mediaStreamLocal.addTrack(videoTrack);

        try {
            peerConnectionLocal.addTrack(videoTrack);
            Log.d("ClientActivity", "Track added to PeerConnection successfully");
        } catch (IllegalStateException e) {
            Log.e("ClientActivity", "Failed to add track to PeerConnection", e);
        }

        Log.d("ClientActivity", "Camera switched successfully");
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
        }, new MediaConstraints());
    }

    private void handleAnswer(SessionDescription sessionDescription) {
        peerConnectionLocal.setRemoteDescription(new SdpAdapter("local set remote"), sessionDescription);
    }

    private void handleIceCandidate(IceCandidate iceCandidate) {
        peerConnectionLocal.addIceCandidate(iceCandidate);
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
