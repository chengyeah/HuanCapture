package com.huan.capture;

import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoTrack;

public interface ScreenCaptureCallback {
    void onScreenCaptureReady(VideoCapturer videoTrack);
}
