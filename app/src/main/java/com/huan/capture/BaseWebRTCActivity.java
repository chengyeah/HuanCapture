package com.huan.capture;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoTrack;
import org.webrtc.ext.H264OnlyDecoderFactory;
import org.webrtc.ext.H264OnlyEncoderFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * <br>
 *
 * <br>
 */
public class BaseWebRTCActivity extends BaseTransferActivity {

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

    private EglBase mEglBase;
    private PeerConnectionFactory mConnectionFactory;
    private PeerConnection mConnection;

    private WebRTCStatsMonitor mWebRTCStatsMonitor;

    protected void requestScreenCapture() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    private void startScreenCapture(Intent data) {

        createPeerConnectionFactory();

        ScreenVideoSourceService.setCallback(new ScreenVideoSourceService.Callback() {

            @Override
            public Intent getMediaProjectionPermissionResultData() {
                return data;
            }

            @Override
            public EglBase.Context getEglBaseContext() {
                return mEglBase.getEglBaseContext();
            }

            @Override
            public PeerConnectionFactory getPeerConnectionFactory() {
                return mConnectionFactory;
            }

            @Override
            public void onCreateVideoTrack(VideoTrack tack) {
                createPeerConnection(tack);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ScreenVideoSourceService.class));
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            startScreenCapture(data);
        }
    }

    private void createPeerConnectionFactory() {

        mEglBase = EglBase.create();
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions
                        .builder(this)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions()
        );

        EglBase.Context eglBaseContext = mEglBase.getEglBaseContext();
        VideoEncoderFactory videoEncoderFactory =
                new DefaultVideoEncoderFactory(eglBaseContext, true, true);
        VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(eglBaseContext);
        videoEncoderFactory = new H264OnlyEncoderFactory(eglBaseContext);
        videoDecoderFactory = new H264OnlyDecoderFactory(eglBaseContext);

        mConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .createPeerConnectionFactory();

        Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);

        VideoCodecInfo[] encodeCodecs = videoEncoderFactory.getSupportedCodecs();
        for (VideoCodecInfo codec : encodeCodecs) {
            Log.d("sunrain", "encodeCodecs " + codec.toString());
        }

        VideoCodecInfo[] decodeCodecs = videoDecoderFactory.getSupportedCodecs();
        for (VideoCodecInfo codec : decodeCodecs) {
            Log.d("sunrain", "decodeCodecs " + codec.toString());
        }
    }

    private void createPeerConnection(VideoTrack screenCapTack) {

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        mConnection = mConnectionFactory.createPeerConnection(
                iceServers,
                new PeerConnectionAdapter("screenCapAdapter") {
                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        super.onIceCandidate(iceCandidate);
                        sendIceCandidateToTV(iceCandidate);
                    }
                }
        );

        if (mConnection == null) {
            throw new RuntimeException("创建PeerConnection失败");
        }

        mConnection.addTrack(screenCapTack); // 替代 addStream

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", "20"));
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", "20"));
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", "20"));

        mConnection.createOffer(new SdpAdapter("OFFER") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {

                String originSdp1 = sdp.description;
//                sdp = new SessionDescription(SessionDescription.Type.OFFER, preferCodec(sdp.description));
                String originSdp2 = sdp.description;

                Log.d("sunrain", "协商 原始 手机 +++++++++++++=== " + originSdp1);
                Log.d("sunrain", "协商 手机 +++++++++++++=== " + originSdp2);

                super.onCreateSuccess(sdp);
                mConnection.setLocalDescription(new SdpAdapter("RE_DSP"), sdp);
                sendOfferToTV(sdp);
            }
        }, constraints);

        mWebRTCStatsMonitor = new WebRTCStatsMonitor(mConnection, 1000);
        mWebRTCStatsMonitor.start();
    }

    @Override
    protected void onReceiveAnswer(SessionDescription sdp) {
        super.onReceiveAnswer(sdp);
        Log.d("TAG", "协商 TV +++++++++++++=== " + sdp.description);
        mConnection.setRemoteDescription(new SdpAdapter("ANSWER"), sdp);
    }

    private String preferCodec(String sdp) {
//        String[] lines = sdp.split("\r\n");
//        for (int i = 0; i < lines.length; i++) {
//            if(lines[i].startsWith("m=video")) {
//                lines[i] = "m=video 9 UDP/TLS/RTP/SAVPF 96";
//                continue;
//            }
//
//            if(lines[i].startsWith("a=rtpmap:96")) {
//                lines[i] = "a=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f;max-fr=20;max-fs=8160";
//                continue;
//            }
//
//            if(lines[i].contains("msid:-")) {
//                lines[i] = lines[i].replace("msid:-", "msid:default");
//                continue;
//            }
//        }
//        return String.join("\r\n", lines);
        return sdp.replace("m=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100", "m=video 9 UDP/TLS/RTP/SAVPF 96");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mWebRTCStatsMonitor != null) {
            mWebRTCStatsMonitor.stop();
            mWebRTCStatsMonitor = null;
        }

        if (mConnection != null) {
            mConnection.dispose();
            mConnection = null;
        }

        if (mConnectionFactory != null) {
            mConnectionFactory.dispose();
            mConnectionFactory = null;
        }

        stopService(new Intent(this, ScreenVideoSourceService.class));
    }
}
