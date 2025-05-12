package com.huan.capture;

import org.webrtc.VideoTrack;

public interface ScreenCaptureCallback {
    void onScreenCaptureReady(VideoTrack videoTrack);
}
