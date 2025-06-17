package com.huan.capture;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;
import org.webrtc.h264.H264OnlyDecoderFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * TV 服务端+webrtc接收端
 */
public class TVActivity extends AppCompatActivity {
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnectionRemote;
    private SurfaceViewRenderer remoteView;
    private EglBase eglBase;
    private WebSocketManager mWebSocketManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv);

        initSocket();

        eglBase = EglBase.create();

        // 第一步：创建PeerConnectionFactory
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                .builder(this)
                .createInitializationOptions());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoDecoderFactory(new H264OnlyDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();

        remoteView = findViewById(R.id.remoteView);
        remoteView.setMirror(false);
        remoteView.init(eglBase.getEglBaseContext(), null);

        call();
    }

    @SuppressLint("SetTextI18n")
    private void initSocket() {
        mWebSocketManager = new WebSocketManager(this);
        mWebSocketManager.startServer(Config.SOCKET_IP, 38383);
        mWebSocketManager.setOnOfferReceivedListener(this::handleOffer);
        mWebSocketManager.setOnAnswerReceivedListener(this::handleAnswer);
        mWebSocketManager.setOnIceCandidateReceivedListener(this::handleIceCandidate);

    }

    private void call() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        peerConnectionRemote = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnectionAdapter("remoteconnection") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                // 发送 ICE 到手机端
                sendIceCandidateToLocal(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                VideoTrack localVideoTrack = mediaStream.videoTracks.get(0);
                runOnUiThread(() -> {
                    localVideoTrack.addSink(remoteView);
                });
            }

            @Override
            public void onTrack(RtpTransceiver transceiver) {
                super.onTrack(transceiver);
                Log.i("--==>", "onTrack");
                MediaStreamTrack track = transceiver.getReceiver().track();
                if (track instanceof VideoTrack) {
                    runOnUiThread(() -> ((VideoTrack) track).addSink(remoteView));
                }
            }
        });
    }

    private void sendIceCandidateToLocal(IceCandidate iceCandidate) {
        // 实现发送 iceCandidate 到本地的逻辑
        // 例如通过 WebSocket 或其他通信方式
        mWebSocketManager.sendIceCandidate(iceCandidate);
    }

    private void sendSessionDescriptionToLocal(SessionDescription sessionDescription) {
        // 实现发送 sessionDescription 到本地的逻辑
        // 例如通过 WebSocket 或其他通信方式
        mWebSocketManager.sendAnswer(sessionDescription);
    }

    private void handleOffer(String sdp) {
        peerConnectionRemote.setRemoteDescription(new SdpAdapter("remote set remote"), new SessionDescription(SessionDescription.Type.OFFER, sdp));
        peerConnectionRemote.createAnswer(new SdpAdapter("remote answer sdp") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                Log.i("--==>", "创建createAnswer收到");
                super.onCreateSuccess(sdp);
                peerConnectionRemote.setLocalDescription(new SdpAdapter("remote set local"), sdp);
                sendSessionDescriptionToLocal(sdp);
            }
        }, new MediaConstraints());
    }

    private void handleAnswer(String sdp) {
        // 处理 answer
        Log.i("--==>", "收到answer");
    }

    private void handleIceCandidate(String sdpMid, int sdpMLineIndex, String candidate) {
        Log.i("--==>", "收到IceCandidate");
        peerConnectionRemote.addIceCandidate(new IceCandidate(sdpMid, sdpMLineIndex, candidate));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        eglBase.release();
        mWebSocketManager.stopServer();
    }
}
