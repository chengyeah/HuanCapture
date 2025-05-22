package com.huan.capture;

import static org.webrtc.SessionDescription.Type.ANSWER;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStats;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.factory.H264OnlyVideoDecoderFactory;
import org.webrtc.factory.H264OnlyVideoEncoderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import eskit.sdk.support.messenger.client.EsMessenger;
import eskit.sdk.support.messenger.client.bean.EsEvent;
import eskit.sdk.support.messenger.client.core.EsCommand;

public class ScreenActivity extends AppCompatActivity {
    private final MediaConstraints mediaConstraints = new MediaConstraints();
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnectionLocal;
    private SurfaceViewRenderer localView;
    private EglBase eglBase;
    private boolean isOpenPermission = false;
    private VideoCapturer videoCapturer;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private MediaProjectionManager projectionManager;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private Intent serviceIntent;
    private Timer statsTimer;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);

        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "0"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "0"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("VideoCodec", "H264"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("MaxBitrateBps", "800000"));

        ConfigParams.getInstance().setOnClientMessageListener(new ConfigParams.OnClientMessageListener() {
            @Override
            public void onMessage(EsEvent esEvent) {
                Gson gson = new Gson();
                ActionBean actionBean = gson.fromJson(esEvent.getData(), ActionBean.class);
                switch (actionBean.getAction()) {
                    case "answer":
                        ActionBean.ActionBeanTDO actionBeanTDO = gson.fromJson(actionBean.getArgs(), ActionBean.ActionBeanTDO.class);
                        Log.i("--==>", "收到 answer is : " + actionBeanTDO.getSdp());
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
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new H264OnlyVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new H264OnlyVideoEncoderFactory(eglBase.getEglBaseContext()))
                .setOptions(options)
                .createPeerConnectionFactory();

        localView = findViewById(R.id.localView);
        localView.setMirror(false);
        localView.init(eglBase.getEglBaseContext(), null);

        initView();

        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            Log.d("--==>", "解码器: " + codecInfo.getName());
        }
    }

    private void initView() {
        VideoView videoView = findViewById(R.id.videoView);
        videoView.setVideoURI(Uri.parse("https://cdn.ryplay10.com/20250518/41280_78e877d9/index.m3u8"));
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
        });
        videoView.start();

        Button btnCall = findViewById(R.id.btnCall);
        btnCall.setOnClickListener(view -> {
            if (!isOpenPermission) {
                Toast.makeText(this, "请先打开权限", Toast.LENGTH_SHORT).show();
                return;
            }
            call();
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
            isOpenPermission = true;
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

    private void initScreenCapture() {
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(720, 1080, 15);

        videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        MediaStream mediaStreamLocal = peerConnectionFactory.createLocalMediaStream("mediaStreamLocal");
        mediaStreamLocal.addTrack(videoTrack);

        if (peerConnectionLocal != null) {
            try {
                peerConnectionLocal.addTrack(videoTrack);
            } catch (Exception e) {
                Log.e("ScreenActivity", "添加屏幕轨道失败", e);
            }
        }
    }

    private void call() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        peerConnectionLocal = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnectionAdapter("localconnection") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Log.i("--==>", "接收到onIceCandidate回调 is：" + iceCandidate.toString());
                // 发送 ICE 到 TV 端
                sendIceCandidateToTV(iceCandidate);
            }
        });

        VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("video", videoSource);
        peerConnectionLocal.addTrack(videoTrack);

        peerConnectionLocal.createOffer(new SdpAdapter("local offer sdp") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnectionLocal.setLocalDescription(new SdpAdapter("local set local", () -> {
                    // 发送 offer 到 TV 端
                    sendOfferToTV(sessionDescription);
                }), sessionDescription);

            }
        }, mediaConstraints);

        startStatsLogging(peerConnectionLocal);
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
        Log.i("--==>", "处理后offer数据 : " + sdpFormat);
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
                videoTrack.dispose();
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

        if (statsTimer != null) {
            statsTimer.cancel();
            statsTimer = null;
        }
    }

    private void startStatsLogging(PeerConnection peerConnection) {
        statsTimer = new Timer();
        statsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                peerConnection.getStats(reports -> {
                    for (RTCStats report : reports.getStatsMap().values()) {
                        parseStatsReport(report);
                    }
                });
            }
        }, 0, 1000);
    }

    private void parseStatsReport(RTCStats report) {
        String type = report.getType();
        Map<String, Object> values = report.getMembers();

        if (type.equals("inbound-rtp")) {
            Object framesDecoded = values.get("framesDecoded");
            Object framesDropped = values.get("framesDropped");
            Object jitter = values.get("jitter");
            Object packetsLost = values.get("packetsLost");
            Object totalDecodeTime = values.get("totalDecodeTime");
            Object decoderImplementation = values.get("decoderImplementation");

            Log.d("RTC_STATS", "[inbound-rtp] " +
                    "framesDecoded=" + framesDecoded +
                    ", framesDropped=" + framesDropped +
                    ", jitter=" + jitter +
                    ", packetsLost=" + packetsLost +
                    ", decodeTime=" + totalDecodeTime +
                    ", decoder=" + decoderImplementation);
        }

        if (type.equals("outbound-rtp")) {
            Object framesEncoded = values.get("framesEncoded");
            Object totalEncodeTime = values.get("totalEncodeTime");
            Object qpSum = values.get("qpSum");
            Object encoderImplementation = values.get("encoderImplementation");

            Log.d("RTC_STATS", "[outbound-rtp] " +
                    "framesEncoded=" + framesEncoded +
                    ", encodeTime=" + totalEncodeTime +
                    ", encoder=" + encoderImplementation +
                    ", qpSum=" + qpSum);
        }

        if (type.equals("candidate-pair")) {
            Object currentRoundTripTime = values.get("currentRoundTripTime");
            Object totalRoundTripTime = values.get("totalRoundTripTime");
            Object availableOutgoingBitrate = values.get("availableOutgoingBitrate");
            Object availableIncomingBitrate = values.get("availableIncomingBitrate");

            Log.d("RTC_STATS", "[candidate-pair] " +
                    "RTT=" + currentRoundTripTime +
                    ", outBitrate=" + availableOutgoingBitrate +
                    ", inBitrate=" + availableIncomingBitrate);
        }
    }
}
