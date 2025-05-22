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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <br>
 *
 * <br>
 */
public class BaseWebRTCActivity extends BaseTransferActivity {

    private static final String TAG = "[-WebRTCActivity-]";

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
                        .builder(getApplicationContext())
                        .setEnableInternalTracer(true)
                        .createInitializationOptions()
        );

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = 0;
        EglBase.Context eglBaseContext = mEglBase.getEglBaseContext();
        VideoEncoderFactory videoEncoderFactory =
                new DefaultVideoEncoderFactory(eglBaseContext, true, true);
        VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(eglBaseContext);
        videoEncoderFactory = new H264OnlyEncoderFactory(eglBaseContext);
        videoDecoderFactory = new H264OnlyDecoderFactory(eglBaseContext);

        mConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .setOptions(options)
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

        mConnection.addTrack(screenCapTack);

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", "20"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));

        mConnection.createOffer(new SdpAdapter("OFFER") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {

                String originSdp1 = sdp.description;
                sdp = new SessionDescription(SessionDescription.Type.OFFER, preferCodec(sdp.description, "H264", false));
                String originSdp2 = sdp.description;

                Log.d("sunrain", "协商 原始 手机 +++++++++++++=== " + originSdp1);
                Log.d("sunrain", "协商 手机 +++++++++++++=== " + originSdp2);

                super.onCreateSuccess(sdp);
                mConnection.setLocalDescription(new SdpAdapter("RE_DSP"), sdp);
                sendOfferToTV(sdp);
            }
        }, constraints);

//        mWebRTCStatsMonitor = new WebRTCStatsMonitor(mConnection, 1000);
//        mWebRTCStatsMonitor.start();
    }

    @Override
    protected void onReceiveAnswer(SessionDescription sdp) {
        super.onReceiveAnswer(sdp);
        Log.d("TAG", "协商 TV +++++++++++++=== " + sdp.description);
        mConnection.setRemoteDescription(new SdpAdapter("ANSWER"), sdp);
    }

    private static String movePayloadTypesToFront(List<String> preferredPayloadTypes, String mLine) {
        // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
        final List<String> origLineParts = Arrays.asList(mLine.split(" "));
        if (origLineParts.size() <= 3) {
            Log.e(TAG, "Wrong SDP media description format: " + mLine);
            return null;
        }
        final List<String> header = origLineParts.subList(0, 3);
        final List<String> unpreferredPayloadTypes =
                new ArrayList<String>(origLineParts.subList(3, origLineParts.size()));
        unpreferredPayloadTypes.removeAll(preferredPayloadTypes);
        // Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
        // types.
        final List<String> newLineParts = new ArrayList<String>();
        newLineParts.addAll(header);
        newLineParts.addAll(preferredPayloadTypes);
        newLineParts.addAll(unpreferredPayloadTypes);
        return joinString(newLineParts, " ", false /* delimiterAtEnd */);
    }

    private static String preferCodec(String sdpDescription, String codec, boolean isAudio) {
        final String[] lines = sdpDescription.split("\r\n");
        final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
        if (mLineIndex == -1) {
            Log.w(TAG, "No mediaDescription line, so can't prefer " + codec);
            return sdpDescription;
        }
        // A list with all the payload types with name |codec|. The payload types are integers in the
        // range 96-127, but they are stored as strings here.
        final List<String> codecPayloadTypes = new ArrayList<String>();
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
        for (int i = 0; i < lines.length; ++i) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecPayloadTypes.add(codecMatcher.group(1));
            }
        }
        if (codecPayloadTypes.isEmpty()) {
            Log.w(TAG, "No payload types with name " + codec);
            return sdpDescription;
        }

        final String newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]);
        if (newMLine == null) {
            return sdpDescription;
        }
        Log.d(TAG, "Change media description from: " + lines[mLineIndex] + " to " + newMLine);
        lines[mLineIndex] = newMLine;
        return joinString(Arrays.asList(lines), "\r\n", true /* delimiterAtEnd */);
    }

    private static int findMediaDescriptionLine(boolean isAudio, String[] sdpLines) {
        final String mediaDescription = isAudio ? "m=audio " : "m=video ";
        for (int i = 0; i < sdpLines.length; ++i) {
            if (sdpLines[i].startsWith(mediaDescription)) {
                return i;
            }
        }
        return -1;
    }

    private static String joinString(
            Iterable<? extends CharSequence> s, String delimiter, boolean delimiterAtEnd) {
        Iterator<? extends CharSequence> iter = s.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        if (delimiterAtEnd) {
            buffer.append(delimiter);
        }
        return buffer.toString();
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
