package org.webrtc.ext;

import android.content.Context;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;

/**
 * <br>
 *
 * <br>
 */
public class VideoCaptureAdapter implements VideoCapturer {

    protected VideoCapturer mVideoCapture;

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        mVideoCapture.initialize(surfaceTextureHelper, applicationContext, capturerObserver);
    }

    @Override
    public void startCapture(int width, int height, int framerate) {
        mVideoCapture.startCapture(width, height, framerate);
    }

    @Override
    public void stopCapture() throws InterruptedException {
        mVideoCapture.stopCapture();
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        mVideoCapture.changeCaptureFormat(width, height, framerate);
    }

    @Override
    public void dispose() {
        mVideoCapture.dispose();
    }

    @Override
    public boolean isScreencast() {
        return mVideoCapture.isScreencast();
    }
}
